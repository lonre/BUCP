package com.github.lonre.bucp.plugin.action;

import static java.util.Objects.isNull;
import static java.util.Objects.requireNonNull;

import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction;
import com.intellij.codeInsight.intention.preview.IntentionPreviewInfo;
import com.intellij.execution.filters.TextConsoleBuilderFactory;
import com.intellij.execution.ui.ConsoleView;
import com.intellij.execution.ui.ConsoleViewContentType;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiCodeBlock;
import com.intellij.psi.PsiDeclarationStatement;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementFactory;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiLocalVariable;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifier;
import com.intellij.psi.PsiParserFacade;
import com.intellij.psi.PsiStatement;
import com.intellij.psi.PsiTypes;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.util.PsiTypesUtil;
import com.intellij.util.IncorrectOperationException;
import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import javax.swing.JPanel;
import javax.swing.JTextField;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nls.Capitalization;
import org.jetbrains.annotations.NotNull;

/**
 * @see <a href="https://github.com/JetBrains/intellij-sdk-docs/tree/main/code_samples/conditional_operator_intention/src/main/java/org/intellij/sdk/intention">code_samples</a>
 */
public class BucpAction extends PsiElementBaseIntentionAction {
  public static class BucpParam {
    private final JBPopup popup;
    private String sourceBeanName;

    public BucpParam(JBPopup popup) {
      this.popup = popup;
    }

    public JBPopup getPopup() {
      return popup;
    }

    public String getSourceBeanName() {
      return sourceBeanName;
    }

    public void setSourceBeanName(String sourceBeanName) {
      this.sourceBeanName = sourceBeanName;
    }
  }

  @Override
  public void invoke(@NotNull Project project, Editor editor, @NotNull PsiElement element) throws IncorrectOperationException {
    PsiLocalVariable psiLocalVariable = PsiTreeUtil.getParentOfType(element, PsiLocalVariable.class);
    if (isNull(psiLocalVariable) || !(psiLocalVariable.getParent() instanceof PsiDeclarationStatement)) {
      return;
    }

    this.drawPopup(editor, bucpParam -> {
      WriteCommandAction.runWriteCommandAction(project, () -> doBucp(project, psiLocalVariable, bucpParam));
      bucpParam.getPopup().cancel();
    });
  }

  private void drawPopup(Editor editor, Consumer<BucpParam> c) {
    JPanel panel = new JPanel();
    JTextField textField = new JTextField();
    textField.setColumns(10);
    textField.setRequestFocusEnabled(true);
    textField.setEditable(true);
    textField.setToolTipText("input the source bean name");
    panel.add(textField, BorderLayout.WEST);

    JBPopup popup = JBPopupFactory.getInstance()
        .createComponentPopupBuilder(panel, null)
        .setFocusable(true)
        .setRequestFocus(true)
        .setShowBorder(false)
        .createPopup();
    popup.showInBestPositionFor(editor);
    textField.addActionListener((e) -> {
      BucpParam bucpParam = new BucpParam(popup);
      bucpParam.setSourceBeanName(e.getActionCommand().trim());
      c.accept(bucpParam);
    });
    textField.requestFocus();
  }

  private void doBucp(@NotNull Project project, PsiLocalVariable psiLocalVariable, BucpParam bucpParam) {
    handleWithLocalVariable(psiLocalVariable, bucpParam);
    ConsoleView console = TextConsoleBuilderFactory.getInstance().createBuilder(project).getConsole();
    console.print("Good", ConsoleViewContentType.LOG_INFO_OUTPUT);
  }

  @Override
  public boolean isAvailable(@NotNull Project project, Editor editor, @NotNull PsiElement element) {
    if (isNull(editor)) {
      return false;
    }
    PsiLocalVariable psiLocalVariable = PsiTreeUtil.getParentOfType(element, PsiLocalVariable.class);
    if (isNull(psiLocalVariable)) {
      return false;
    }
    return psiLocalVariable.getParent() instanceof PsiDeclarationStatement;
  }

  @Override
  public @NotNull @Nls(capitalization = Capitalization.Sentence) String getFamilyName() {
    return "BUCP copy bean properties";
  }

  @Override
  public @Nls(capitalization = Capitalization.Sentence) @NotNull String getText() {
    return getFamilyName();
  }

  protected void handleWithLocalVariable(PsiLocalVariable psiLocalVariable, BucpParam bucpParam) {
    String varName = psiLocalVariable.getName();
    PsiClass psiClass = PsiTypesUtil.getPsiClass(psiLocalVariable.getType());
    List<PsiMethod> methodList = extractSetMethods(psiClass);
    if (methodList.isEmpty()) {
      return;
    }

    final Project project = psiLocalVariable.getProject();
    final PsiElementFactory factory = JavaPsiFacade.getInstance(project).getElementFactory();
    PsiElement parentCodeBlock = PsiTreeUtil.getParentOfType(psiLocalVariable, PsiCodeBlock.class); //parent PsiCodeBlock
    PsiElement lastElement = psiLocalVariable.getParent();  // parent PsiDeclarationStatement

    if (isNull(parentCodeBlock)) {
      return;
    }

    for (PsiMethod m : methodList) {
      lastElement = parentCodeBlock.addAfter(PsiParserFacade.getInstance(project).createWhiteSpaceFromText("\n"), lastElement); // PsiJavaToken:SEMICOLON
      lastElement = requireNonNull(PsiTreeUtil.getParentOfType(lastElement, PsiStatement.class)).getNextSibling(); // PsiStatement

      String p = "";
      if (!bucpParam.getSourceBeanName().isEmpty()) {
        p = bucpParam.getSourceBeanName() + "." + this.getterName(m) + "()";
      }

      String code = varName + "." + m.getName() + "(" + p + ");";
      PsiStatement statement = factory.createStatementFromText(code, parentCodeBlock);
      lastElement = parentCodeBlock.addAfter(statement, lastElement);
    }
  }

  private List<PsiMethod> extractSetMethods(PsiClass psiClass) {
    List<PsiMethod> methodList = new ArrayList<>();
    while (isNotSystemClass(psiClass)) {
      methodList.addAll(Arrays.stream(requireNonNull(psiClass).getMethods()).filter(this::isValidSetMethod).toList());
      psiClass = psiClass.getSuperClass();
    }
    return methodList;
  }

  private boolean isNotSystemClass(PsiClass psiClass) {
    if (isNull(psiClass)) {
      return false;
    }
    String qualifiedName = psiClass.getQualifiedName();
    return !isNull(qualifiedName) && !qualifiedName.startsWith("java.");
  }

  private boolean isValidSetMethod(PsiMethod m) {
    return m.hasModifierProperty(PsiModifier.PUBLIC) &&
        !m.hasModifierProperty(PsiModifier.STATIC) &&
        m.getName().startsWith("set") &&
        m.hasParameters() &&
        (m.getParameterList().getParametersCount() == 1) &&
        (m.getName().length() > 3) &&
        Character.isUpperCase(m.getName().charAt(3));
  }

  private String getterName(PsiMethod m) {
    String prefix = PsiTypes.booleanType().equals(requireNonNull(m.getParameterList().getParameters()[0]).getType()) ? "is" : "get";
    return m.getName().replaceFirst("set", prefix);
  }

  @Override
  public @NotNull IntentionPreviewInfo generatePreview(@NotNull Project project, @NotNull Editor editor, @NotNull PsiFile file) {
    return IntentionPreviewInfo.EMPTY;
  }
}

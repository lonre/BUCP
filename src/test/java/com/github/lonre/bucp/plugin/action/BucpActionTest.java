package com.github.lonre.bucp.plugin.action;

import com.github.lonre.bucp.plugin.action.BucpAction.BucpParam;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiLocalVariable;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase;

public class BucpActionTest extends LightJavaCodeInsightFixtureTestCase {
  @Override
  protected String getTestDataPath() {
    return "src/test/testData";
  }

  public void testGo() {
    myFixture.configureByFile("before.template.java");
    PsiElement identifier = myFixture.getFile().findElementAt(myFixture.getEditor().getCaretModel().getOffset());
    PsiLocalVariable psiLocal = PsiTreeUtil.getParentOfType(identifier, PsiLocalVariable.class);
    BucpAction action = new BucpAction();
    BucpParam bucpParam = new BucpParam(null);
    bucpParam.setSourceBeanName("dst");
    WriteCommandAction.runWriteCommandAction(myFixture.getProject(), () -> {
      action.handleWithLocalVariable(psiLocal, bucpParam);
      System.out.println(myFixture.getFile().getText());
    });
  }
}
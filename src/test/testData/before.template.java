public class MyController {
  public User info() {
    User us<caret>er = new User();

    return user;
  }

  public static class User {
    private int id;
    private Long time;
    private String name;
    private Boolean b1;
    private boolean b2;

    public int getId() {
      return id;
    }

    public void setId(int id) {
      this.id = id;
    }

    public Long getTime() {
      return time;
    }

    public void setTime(Long time) {
      this.time = time;
    }

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public Boolean getB1() {
      return b1;
    }

    public void setB1(Boolean b1) {
      this.b1 = b1;
    }

    public boolean isB2() {
      return b2;
    }

    public void setB2(boolean b2) {
      this.b2 = b2;
    }
  }
}
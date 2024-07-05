package io.andrewbarker.swaggerbuilder.swaggermocks;

import java.util.List;

public class MockUser {
  private String name;
  private int userAge;
  private List<String> interests;

  // Getters and Setters
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public int getUserAge() {
    return userAge;
  }

  public void setUserAge(int userAge) {
    this.userAge = userAge;
  }

  public List<String> getInterests() {
    return interests;
  }

  public void setInterests(List<String> interests) {
    this.interests = interests;
  }
}

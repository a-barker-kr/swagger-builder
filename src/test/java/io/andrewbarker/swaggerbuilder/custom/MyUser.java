package io.andrewbarker.swaggerbuilder.custom;

import io.andrewbarker.swaggerbuilder.annotations.MapTo;
import io.andrewbarker.swaggerbuilder.annotations.SwaggerBuilder;
import io.andrewbarker.swaggerbuilder.swaggermocks.MockUser;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@SwaggerBuilder(target = MockUser.class)
public class MyUser {
  private String name;

  @MapTo("userAge")
  private Integer userAge;

  @MapTo("interests")
  private List<String> hobbies;
}

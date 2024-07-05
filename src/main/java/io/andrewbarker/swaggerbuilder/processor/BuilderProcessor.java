package io.andrewbarker.swaggerbuilder.processor;

import com.squareup.javapoet.*;
import io.andrewbarker.swaggerbuilder.annotations.MapTo;
import io.andrewbarker.swaggerbuilder.annotations.SwaggerBuilder;
import io.andrewbarker.swaggerbuilder.annotations.SwaggerBuilders;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Elements;
import javax.tools.Diagnostic;
import java.io.IOException;
import java.io.Writer;
import java.util.List;
import java.util.Set;

@SupportedAnnotationTypes({"io.andrewbarker.swaggerbuilder.annotations.SwaggerBuilder", "io.andrewbarker.swaggerbuilder.annotations.SwaggerBuilders", "io.andrewbarker.swaggerbuilder.annotations.MapTo"})
@SupportedSourceVersion(SourceVersion.RELEASE_21)
public class BuilderProcessor extends AbstractProcessor {

  @Override
  public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
    processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, "BuilderProcessor is running");
    for (Element element : roundEnv.getElementsAnnotatedWith(SwaggerBuilder.class)) {
      if (isClassOrRecord(element)) {
        processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, "Processing element: " + element.getSimpleName());
        SwaggerBuilder annotation = element.getAnnotation(SwaggerBuilder.class);
        TypeMirror targetClass = getTargetClass(annotation);
        generateBuilderClass((TypeElement) element, targetClass, annotation.builderName());
      }
    }
    for (Element element : roundEnv.getElementsAnnotatedWith(SwaggerBuilders.class)) {
      if (isClassOrRecord(element)) {
        SwaggerBuilders buildersAnnotation = element.getAnnotation(SwaggerBuilders.class);
        for (SwaggerBuilder builder : buildersAnnotation.value()) {
          processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, "Processing element: " + element.getSimpleName());
          TypeMirror targetClass = getTargetClass(builder);
          generateBuilderClass((TypeElement) element, targetClass, builder.builderName());
        }
      }
    }
    return true;
  }

  private boolean isClassOrRecord(Element element) {
    return element.getKind() == ElementKind.CLASS || element.getKind() == ElementKind.RECORD;
  }

  private TypeMirror getTargetClass(SwaggerBuilder annotation) {
    try {
      annotation.target(); // This will throw MirroredTypeException
    } catch (MirroredTypeException e) {
      return e.getTypeMirror();
    }
    return null; // This line should never be reached
  }

  private void generateBuilderClass(TypeElement element, TypeMirror targetClass, String builderName) {
    processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, "Generating builder for: " + element.getSimpleName());
    String className = builderName.isEmpty() ? element.getSimpleName() + "Builder" : builderName;
    Elements elementUtils = processingEnv.getElementUtils();
    ClassName builderClassName = ClassName.get(elementUtils.getPackageOf(element).toString(), className);
    TypeSpec.Builder builderClass = TypeSpec.classBuilder(className)
        .addModifiers(javax.lang.model.element.Modifier.PUBLIC);

    MethodSpec.Builder buildMethod = MethodSpec.methodBuilder("build")
        .addModifiers(javax.lang.model.element.Modifier.PUBLIC)
        .returns(ClassName.get(targetClass));

    buildMethod.addStatement("$T target = new $T()", targetClass, targetClass);

    List<? extends Element> enclosedElements = element.getEnclosedElements();
    for (VariableElement field : ElementFilter.fieldsIn(enclosedElements)) {
      String fieldName = field.getSimpleName().toString();
      MapTo mapTo = field.getAnnotation(MapTo.class);
      String targetField = mapTo != null ? mapTo.value() : fieldName;

      // Create FieldSpec
      FieldSpec fieldSpec = FieldSpec.builder(TypeName.get(field.asType()), fieldName, javax.lang.model.element.Modifier.PRIVATE)
          .build();

      builderClass.addField(fieldSpec);

      builderClass.addMethod(MethodSpec.methodBuilder(fieldName)
          .addModifiers(javax.lang.model.element.Modifier.PUBLIC)
          .returns(builderClassName)
          .addParameter(TypeName.get(field.asType()), fieldName)
          .addStatement("this.$N = $N", fieldName, fieldName)
          .addStatement("return this")
          .build());

      buildMethod.addStatement("target.set$L(this.$N)", capitalize(targetField), fieldName);
    }

    buildMethod.addStatement("return target");
    builderClass.addMethod(buildMethod.build());

    JavaFile javaFile = JavaFile.builder(elementUtils.getPackageOf(element).toString(), builderClass.build()).build();

    try (Writer writer = processingEnv.getFiler().createSourceFile(builderClassName.toString()).openWriter()) {
      javaFile.writeTo(writer);
    } catch (IOException e) {
      processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Failed to write builder class: " + e.getMessage());
    }
  }

  private String capitalize(String str) {
    if (str == null || str.length() == 0) {
      return str;
    }
    return str.substring(0, 1).toUpperCase() + str.substring(1);
  }
}

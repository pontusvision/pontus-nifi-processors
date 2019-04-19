package com.pontusvision.nifi.processors;

import org.apache.nifi.components.ValidationContext;
import org.apache.nifi.components.ValidationResult;
import org.apache.nifi.components.Validator;

public class ClassNameValidator implements Validator
{
  ValidationResult.Builder builder = new ValidationResult.Builder();

  @Override public ValidationResult validate(String subject, String input, ValidationContext context)
  {
    ValidationResult.Builder resBuilder = builder.input(input).subject(subject);
    try
    {
      ClassNameValidator.class.getClassLoader().loadClass(input);
    }
    catch (ClassNotFoundException e)
    {
      return resBuilder.explanation("Failed to load Class: " + e.getMessage()).build();
    }

    return resBuilder.explanation("Sucess").valid(true).build();

  }
}

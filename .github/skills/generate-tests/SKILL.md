---
name: generate-tests
description: 'Generate comprehensive unit tests for a component, covering happy path, edge cases, and error conditions'
---

# generate-tests

Generate unit tests for the selected code that:
- Cover all public methods and edge cases.
- Include descriptive test names.
- Use JUnit 5 and Mockito for mocking dependencies.
- Follow the Arrange-Act-Assert pattern for test structure.
- Avoid using SpringBootTest if possible.
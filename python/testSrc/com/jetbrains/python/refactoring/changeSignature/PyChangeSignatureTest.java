package com.jetbrains.python.refactoring.changeSignature;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.refactoring.BaseRefactoringProcessor;
import com.intellij.testFramework.TestDataPath;
import com.jetbrains.python.PyBundle;
import com.jetbrains.python.fixtures.PyTestCase;
import com.jetbrains.python.psi.LanguageLevel;
import com.jetbrains.python.psi.PyFunction;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;

/**
 * User : ktisha
 */
@TestDataPath("$CONTENT_ROOT/../testData/refactoring/changeSignature/")
public class PyChangeSignatureTest extends PyTestCase {

  public void testChangeFunctionName() {
    doChangeSignatureTest("bar", null);
  }

  public void testRemovePositionalParam() {
    doChangeSignatureTest(null, Arrays.asList(new PyParameterInfo(0, "a", null, false)));
  }

  public void testRemoveFirstPositionalParam() {
    doChangeSignatureTest(null, Arrays.asList(new PyParameterInfo(1, "b", null, false)));
  }

  public void testSwitchPositionalParam() {
    doChangeSignatureTest(null, Arrays.asList(new PyParameterInfo(1, "b", null, false), new PyParameterInfo(0, "a", null, false)));
  }

  public void testAddPositionalParam() {
    doChangeSignatureTest(null, Arrays.asList(new PyParameterInfo(0, "a", null, false), new PyParameterInfo(1, "b", null, false),
                                              new PyParameterInfo(-1, "c", "3", false)));
  }

  public void testAddDefaultParam() {
    doChangeSignatureTest(null, Arrays.asList(new PyParameterInfo(0, "a", null, false), new PyParameterInfo(1, "b", null, false),
                                              new PyParameterInfo(-1, "c", "3", true)));
  }

  public void testRemoveDefaultFromParam() {
    doChangeSignatureTest(null, Arrays.asList(new PyParameterInfo(0, "a", null, false), new PyParameterInfo(1, "b", "2", false)));
  }

  public void testAddDefaultParam1() {
    doChangeSignatureTest(null, Arrays.asList(new PyParameterInfo(0, "a", null, false), new PyParameterInfo(-1, "d", "1", true),
                                              new PyParameterInfo(1, "b", "None", true)));
  }

  public void testUpdateDocstring() {
    doChangeSignatureTest(null, Arrays.asList(new PyParameterInfo(0, "a", null, false), new PyParameterInfo(1, "d1", "1", true)));
  }

  public void testFixDocstringRemove() {
    doChangeSignatureTest(null, Arrays.asList(new PyParameterInfo(0, "a", null, false)));
  }

  public void testClassMethod() {
    doChangeSignatureTest(null, Arrays.asList(new PyParameterInfo(0, "self", null, false), new PyParameterInfo(1, "a", null, true),
                                              new PyParameterInfo(-1, "b", "2", false)));
  }

  public void testKeywordParam() {
    doChangeSignatureTest(null, Arrays.asList(new PyParameterInfo(0, "a", null, false),
                                              new PyParameterInfo(-1, "b", "2", false)));
  }

  public void testParamAnnotation() {
    doChangeSignatureTest(null, Arrays.asList(new PyParameterInfo(0, "b", null, false)), LanguageLevel.PYTHON32);
  }

  public void testEmptyParameterName() {
    doValidationTest(null, Arrays.asList(new PyParameterInfo(-1, "", "2", true)),
                     PyBundle.message("refactoring.change.signature.dialog.validation.parameter.missing"));
  }

  public void doChangeSignatureTest(@Nullable String newName, @Nullable List<PyParameterInfo> parameters) {
    myFixture.configureByFile("refactoring/changeSignature/" + getTestName(true) + ".before.py");
    changeSignature(newName, parameters);
    myFixture.checkResultByFile("refactoring/changeSignature/" + getTestName(true) + ".after.py");
  }

  private void doChangeSignatureTest(String newName, @Nullable List<PyParameterInfo> parameters, LanguageLevel level) {
    setLanguageLevel(level);
    try {
      doChangeSignatureTest(newName, parameters);
    }
    finally {
      setLanguageLevel(null);
    }
  }

  public void doValidationTest(@Nullable String newName, @Nullable List<PyParameterInfo> parameters, String expected) {
    myFixture.configureByFile("refactoring/changeSignature/" + getTestName(true) + ".py");
    final PyChangeSignatureHandler changeSignatureHandler = new PyChangeSignatureHandler();
    final PyFunction function = (PyFunction)changeSignatureHandler.findTargetMember(myFixture.getFile(), myFixture.getEditor());
    assertNotNull(function);

    final PyMethodDescriptor method = new PyMethodDescriptor(function);
    final TestPyChangeSignatureDialog dialog = new TestPyChangeSignatureDialog(function.getProject(), method);
    if (newName != null) {
      dialog.setNewName(newName);
    }
    if (parameters != null) {
      dialog.setParameterInfos(parameters);
    }

    final String validationError = dialog.validateAndCommitData();
    assertEquals(expected, validationError);
  }

  private void changeSignature(@Nullable String newName, @Nullable List<PyParameterInfo> parameters) {
    final PyChangeSignatureHandler changeSignatureHandler = new PyChangeSignatureHandler();
    final PyFunction function = (PyFunction)changeSignatureHandler.findTargetMember(myFixture.getFile(), myFixture.getEditor());
    assertNotNull(function);

    final PyMethodDescriptor method = new PyMethodDescriptor(function);
    final TestPyChangeSignatureDialog dialog = new TestPyChangeSignatureDialog(function.getProject(), method);
    if (newName != null) {
      dialog.setNewName(newName);
    }
    if (parameters != null) {
      dialog.setParameterInfos(parameters);
    }

    final String validationError = dialog.validateAndCommitData();
    assertTrue(validationError, validationError == null);

    final BaseRefactoringProcessor baseRefactoringProcessor = dialog.createRefactoringProcessor();
    assert baseRefactoringProcessor instanceof PyChangeSignatureProcessor;

    final PyChangeSignatureProcessor processor = (PyChangeSignatureProcessor)baseRefactoringProcessor;
    processor.run();
    Disposer.dispose(dialog.getDisposable());
  }


  public static class TestPyChangeSignatureDialog extends PyChangeSignatureDialog {

    public TestPyChangeSignatureDialog(Project project, PyMethodDescriptor method) {
      super(project, method);
    }

    public void setNewName(String newName) {
      myNameField.setText(newName);
    }
  }
}

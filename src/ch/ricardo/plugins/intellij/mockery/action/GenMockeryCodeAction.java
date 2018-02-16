package ch.ricardo.plugins.intellij.mockery.action;

import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.actionSystem.EditorAction;
import com.intellij.psi.PsiFile;

public class GenMockeryCodeAction extends EditorAction {

    public static final String GOLANG_FILE_NAME_SUFFIX = ".go";

    public GenMockeryCodeAction() {
        super(new GenMockeryActionHandler());
    }

    /**
     * Enables action for test Java files only.
     */
    @Override
    public void update(Editor editor, Presentation presentation, DataContext dataContext) {
        PsiFile psiFile = (PsiFile) dataContext.getData(CommonDataKeys.PSI_FILE.getName());
        boolean enabled = false;
        if (psiFile != null) {
            enabled = psiFile.getName().endsWith(GOLANG_FILE_NAME_SUFFIX);
        }
        presentation.setEnabled(enabled);
    }
}

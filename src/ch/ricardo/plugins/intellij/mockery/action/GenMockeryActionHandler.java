package ch.ricardo.plugins.intellij.mockery.action;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.ExecutionHelper;
import com.intellij.execution.ExecutionModes;
import com.intellij.execution.OutputListener;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.configurations.PtyCommandLine;
import com.intellij.execution.process.KillableColoredProcessHandler;
import com.intellij.execution.process.OSProcessHandler;
import com.intellij.execution.process.ProcessEvent;
import com.intellij.execution.process.ProcessListener;
import com.intellij.ide.projectView.ProjectView;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.actionSystem.EditorWriteActionHandler;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vcs.changes.ui.ChangesTreeImpl;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.util.EnvironmentUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;

public class GenMockeryActionHandler extends EditorWriteActionHandler {
    @Override
    public void executeWriteAction(Editor editor, Caret caret, DataContext dataContext) {

        final StringBuilder out = new StringBuilder();
        final StringBuilder err = new StringBuilder();

        String selectedText = editor.getSelectionModel().getSelectedText();

        if (selectedText == null || selectedText.isEmpty()) {

            JTextArea jTextArea = new JTextArea("You should select the interface you want to mock");
            JComponent myPanel = new JPanel();
            myPanel.add(jTextArea);

            JBPopupFactory.getInstance()
                    .createComponentPopupBuilder(myPanel, null)
                    .setCancelOnClickOutside(true)
                    .setRequestFocus(true)
                    .setResizable(true)
                    .setMayBeParent(true)
                    .createPopup()
                    .showInFocusCenter();

            return;
        }

        Path filePath = Paths.get(FileDocumentManager.getInstance().getFile(editor.getDocument()).getCanonicalPath());

        GeneralCommandLine commandLine = PtyCommandLine.isEnabled() ? new PtyCommandLine() : new GeneralCommandLine();
        commandLine.withWorkDirectory(filePath.getParent().toString());
        commandLine.setExePath(EnvironmentUtil.getValue("GOPATH") + "/bin/mockery");
        commandLine.addParameters("-name", selectedText);

        OSProcessHandler processHandler;

        try {
            processHandler = new KillableColoredProcessHandler(commandLine);
        } catch (ExecutionException e) {
            ExecutionHelper.showErrors(editor.getProject(), Collections.singletonList(e), commandLine.getCommandLineString(), null);
            return;
        }

        processHandler.addProcessListener(new OutputListener(out, err));
        processHandler.addProcessListener(new ProcessListener() {
            @Override
            public void startNotified(@NotNull ProcessEvent processEvent) {

            }

            @Override
            public void processTerminated(@NotNull ProcessEvent processEvent) {
                VfsUtil.markDirtyAndRefresh(true, true, true, editor.getProject().getBaseDir());
            }

            @Override
            public void processWillTerminate(@NotNull ProcessEvent processEvent, boolean b) {

            }

            @Override
            public void onTextAvailable(@NotNull ProcessEvent processEvent, @NotNull Key key) {

            }
        });
        processHandler.startNotify();
        ExecutionHelper.executeExternalProcess(editor.getProject(), processHandler, new ExecutionModes.BackGroundMode("mockery generation for " + selectedText), commandLine);

    }
}

package ch.ricardo.plugins.intellij.mockery.action;

import com.intellij.codeInsight.hint.HintManager;
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
import com.intellij.ide.util.ChooseElementsDialog;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.actionSystem.EditorActionHandler;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.util.EnvironmentUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.StringJoiner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GenMockeryActionHandler extends EditorActionHandler {

    private class InterfaceChooser extends ChooseElementsDialog<String> {

        private InterfaceChooser (Project project, List<String> interfaces) {
            super(project, interfaces, "Generate mockery", "Select the interfaces you want to generate mocks for", true);
        }
        @Override
        protected String getItemText(String s) {
            return s;
        }

        @Nullable
        @Override
        protected Icon getItemIcon(String s) {
            return null;
        }
    }


    @Override
    public void doExecute(Editor editor, Caret caret, DataContext dataContext) {

        final StringBuilder out = new StringBuilder();
        final StringBuilder err = new StringBuilder();

        ArrayList<String> interfaces = new ArrayList<>();
        for (String s : editor.getDocument().getText().split("\\r?\\n")) {
            Pattern pattern = Pattern.compile("type (.*?) interface");
            Matcher matcher = pattern.matcher(s);
            if (matcher.find())
            {
                interfaces.add(matcher.group(1));
            }
        }

        HintManager.getInstance().showErrorHint(editor, "not supported");

        if (interfaces.isEmpty()) {

            HintManager.getInstance().showErrorHint(editor, "No interfaces found");
            return;
        }

        InterfaceChooser chooser = new InterfaceChooser(editor.getProject(), interfaces);
        chooser.selectElements(interfaces);
        List<String> selectedInterfaces = chooser.showAndGetResult();

        if (selectedInterfaces.isEmpty()) {
            return;
        }

        Path filePath = Paths.get(FileDocumentManager.getInstance().getFile(editor.getDocument()).getCanonicalPath());

        GeneralCommandLine commandLine = PtyCommandLine.isEnabled() ? new PtyCommandLine() : new GeneralCommandLine();
        commandLine.withWorkDirectory(filePath.getParent().toString());
        commandLine.setExePath(EnvironmentUtil.getValue("GOPATH") + "/bin/mockery");

        StringJoiner joiner = new StringJoiner("|");
        for (String s : selectedInterfaces) {
            joiner.add("^"+s+"$");
        }
        commandLine.addParameters("-name", joiner.toString());

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
        ExecutionHelper.executeExternalProcess(editor.getProject(), processHandler, new ExecutionModes.BackGroundMode("mockery generation"), commandLine);

    }
}



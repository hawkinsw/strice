package cr.obs.strice;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.lang.Language;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.editor.SelectionModel;
import org.jetbrains.annotations.NotNull;
import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import org.apache.commons.text.StringSubstitutor;
import org.apache.commons.text.StringEscapeUtils;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;


public class StriceAction extends AnAction {
    static String compilerExplorerStateTemplate = "{ \"sessions\": [ { \"id\": 1, \"compilers\": [], \"language\": \"${language}\", \"source\": \"${source}\" } ] }";
    static String compilerExplorerStateBaseUrl = "https://www.godbolt.org/clientstate/";

    @Override
    public void update(@NotNull AnActionEvent e) {
        // We only want to enable the plugin if we have an editor *and*
        // the user has selected text.

        Editor editor = e.getData(CommonDataKeys.EDITOR);
        if (editor == null) {
            e.getPresentation().setEnabledAndVisible(false);
            return;
        }
        SelectionModel selectionModel = editor.getSelectionModel();
        if (!selectionModel.hasSelection()) {
            e.getPresentation().setEnabledAndVisible(false);
        }
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Editor editor = e.getData(CommonDataKeys.EDITOR);
        assert (editor != null);

        // If there is nothing selected, we will do nothing.
        SelectionModel selectionModel = editor.getSelectionModel();
        if (!selectionModel.hasSelection()) {
            return;
        }

        // Use the string substitutor to configure the state template
        // with the user-highlighted text.
        Map<String, String> substitutorMap = new HashMap<>();
        StringSubstitutor clientStateBuilder = new StringSubstitutor(substitutorMap);

        // If the IDE can tell what language the user is coding, extract that so
        // that we can properly configure Compiler Explorer.
        substitutorMap.put("language", "");
        Language lang = e.getData(CommonDataKeys.LANGUAGE);
        if (lang != null) {
            substitutorMap.put("language", lang.getDisplayName().toLowerCase());
        }

        // Before we can embed the source code into the JSON, we will have to
        // make sure that everything is properly escaped.
        substitutorMap.put("source", StringEscapeUtils.escapeJson(selectionModel.getSelectedText()));

        // Now, build the state and base64 encode.
        String state = clientStateBuilder.replace(compilerExplorerStateTemplate);
        String encodedState = Base64.getEncoder().encodeToString(state.getBytes(StandardCharsets.UTF_8));

        String compilerExplorerUrl = compilerExplorerStateBaseUrl + encodedState;
        BrowserUtil.browse(compilerExplorerUrl);
    }
}

package com.voidvvv.kzcollision.editor;

import com.voidvvv.kzcollision.editor.project.AssetIndex;
import com.voidvvv.kzcollision.editor.project.EditorProjectContext;
import com.voidvvv.kzcollision.editor.project.ResourceRecoveryReport;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;

public class EditorStateProjectContextTest {
    @Rule
    public final TemporaryFolder temp = new TemporaryFolder();

    @Test
    public void reset_shouldClearProjectContextIndexAndReport() throws Exception {
        File assets = temp.newFolder();
        EditorState state = new EditorState();
        state.setProjectContext(new EditorProjectContext(null, assets,
                new File(assets, "collision/collision.json"), false));
        state.setAssetIndex(new AssetIndex(assets));
        state.setRecoveryReport(new ResourceRecoveryReport());

        state.reset();

        Assert.assertNull(state.getProjectContext());
        Assert.assertNull(state.getAssetIndex());
        Assert.assertNull(state.getRecoveryReport());
    }
}

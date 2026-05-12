package com.voidvvv.kzcollision.editor.panels;

import java.io.File;

/**
 * Callback interface for file operations triggered from the PanelManager menu bar.
 * Implemented by KZCollisionEditor to handle save/load/export actions.
 */
public interface FileMenuHandler {
    /** Create a new empty project, discarding current work. */
    void newProject();

    /** Load a project from the given file, replacing the current state. */
    void openProject(File file);

    /** Save the current project to the given file. */
    void saveProject(File file);

    /** Export collision-relevant data as JSON to the given file. */
    void exportCollisionJson(File file);

    /** Save to the currently tracked file, or prompt via the handler if none set. */
    void saveCurrentProject();

    /** Returns true if there is a current file tracked for quick-save. */
    boolean hasCurrentFile();
}

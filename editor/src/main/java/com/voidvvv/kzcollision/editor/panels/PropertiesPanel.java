package com.voidvvv.kzcollision.editor.panels;

import com.voidvvv.kzcollision.core.model.AnimationFrame;
import com.voidvvv.kzcollision.core.model.CollisionBox;
import com.voidvvv.kzcollision.editor.EditorState;
import imgui.ImGui;
import imgui.type.ImFloat;
import imgui.type.ImString;

import java.util.List;

public class PropertiesPanel {
    private final EditorStateProvider stateProvider;
    private final ImFloat originXBuf = new ImFloat(0);
    private final ImFloat originYBuf = new ImFloat(0);
    private final ImFloat boxXBuf = new ImFloat(0);
    private final ImFloat boxYBuf = new ImFloat(0);
    private final ImFloat boxWBuf = new ImFloat(32);
    private final ImFloat boxHBuf = new ImFloat(32);
    private final ImString labelBuf = new ImString(32);

    public PropertiesPanel(EditorStateProvider stateProvider) {
        this.stateProvider = stateProvider;
    }

    public void render() {
        EditorState state = stateProvider.getState();
        AnimationFrame frame = state.getCurrentFrame();

        if (ImGui.begin("Properties")) {
            if (frame == null) {
                ImGui.text("No frame selected");
                ImGui.end();
                return;
            }

            // Origin section
            ImGui.text("Frame Origin");
            originXBuf.set(frame.getOriginX());
            originYBuf.set(frame.getOriginY());
            if (ImGui.inputFloat("Origin X", originXBuf, 1f, 10f, "%.1f")) {
                frame.setOriginX(originXBuf.get());
            }
            if (ImGui.inputFloat("Origin Y", originYBuf, 1f, 10f, "%.1f")) {
                frame.setOriginY(originYBuf.get());
            }

            ImGui.separator();

            // Collision boxes section
            ImGui.text("Collision Boxes");
            List<CollisionBox> boxes = frame.getCollisionBoxes();
            String selectedBoxId = state.getSelectedCollisionBoxId();

            for (int i = 0; i < boxes.size(); i++) {
                CollisionBox box = boxes.get(i);
                boolean isSelected = box.getId().equals(selectedBoxId);

                ImGui.pushID(box.getId());

                String displayLabel = box.getLabel() != null ? box.getLabel() : "Box " + i;
                if (ImGui.selectable(displayLabel, isSelected)) {
                    state.setSelectedCollisionBoxId(box.getId());
                    // Load box values into buffers
                    boxXBuf.set(box.getX());
                    boxYBuf.set(box.getY());
                    boxWBuf.set(box.getWidth());
                    boxHBuf.set(box.getHeight());
                    labelBuf.set(box.getLabel() != null ? box.getLabel() : "");
                }

                // Show edit fields for selected box
                if (isSelected) {
                    ImGui.indent();
                    if (ImGui.inputFloat("X", boxXBuf, 1f, 10f, "%.1f")) box.setX(boxXBuf.get());
                    if (ImGui.inputFloat("Y", boxYBuf, 1f, 10f, "%.1f")) box.setY(boxYBuf.get());
                    if (ImGui.inputFloat("W", boxWBuf, 1f, 10f, "%.1f")) box.setWidth(Math.max(1, boxWBuf.get()));
                    if (ImGui.inputFloat("H", boxHBuf, 1f, 10f, "%.1f")) box.setHeight(Math.max(1, boxHBuf.get()));
                    ImGui.inputText("Label", labelBuf);
                    box.setLabel(labelBuf.get().isEmpty() ? null : labelBuf.get());

                    ImGui.sameLine();
                    if (ImGui.button("Remove")) {
                        boxes.remove(i);
                        state.setSelectedCollisionBoxId(null);
                        ImGui.unindent();
                        ImGui.popID();
                        break;
                    }
                    ImGui.unindent();
                }

                ImGui.popID();
            }

            ImGui.separator();
            if (ImGui.button("+ Add Box")) {
                CollisionBox newBox = new CollisionBox(-16, 0, 32, 32, "new_box");
                boxes.add(newBox);
                state.setSelectedCollisionBoxId(newBox.getId());
                boxXBuf.set(newBox.getX());
                boxYBuf.set(newBox.getY());
                boxWBuf.set(newBox.getWidth());
                boxHBuf.set(newBox.getHeight());
                labelBuf.set(newBox.getLabel());
            }
        }
        ImGui.end();
    }
}

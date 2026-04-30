package com.voidvvv.kzcollision.editor.viewport;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL30;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.voidvvv.kzcollision.core.model.AnimationFrame;
import com.voidvvv.kzcollision.core.model.CollisionBox;
import com.voidvvv.kzcollision.core.model.Rect;
import com.voidvvv.kzcollision.core.model.SourceAsset;
import com.voidvvv.kzcollision.core.model.SpriteFrame;
import com.voidvvv.kzcollision.editor.EditorState;

import java.util.Map;

public class ViewportRenderer {
    private final SpriteBatch batch;
    private final ShapeRenderer shapes;
    private final ViewportCamera camera;
    private final EditorState state;

    public ViewportRenderer(EditorState state) {
        this.state = state;
        this.batch = new SpriteBatch();
        this.shapes = new ShapeRenderer();
        this.camera = new ViewportCamera();
    }

    public ViewportCamera getCamera() {
        return camera;
    }

    public void render(float viewportX, float viewportY, float viewportWidth, float viewportHeight) {
        // Set viewport (scissor + viewport) using OpenGL coordinates (bottom-left origin)
        Gdx.gl.glViewport((int) viewportX, (int) viewportY, (int) viewportWidth, (int) viewportHeight);
        Gdx.gl.glEnable(GL30.GL_SCISSOR_TEST);
        Gdx.gl.glScissor((int) viewportX, (int) viewportY, (int) viewportWidth, (int) viewportHeight);

        // Clear viewport area
        Gdx.gl.glClearColor(0.12f, 0.12f, 0.18f, 1f);
        Gdx.gl.glClear(GL30.GL_COLOR_BUFFER_BIT);

        // Setup camera
        OrthographicCamera cam = new OrthographicCamera(viewportWidth, viewportHeight);
        cam.translate(camera.getOffsetX(), camera.getOffsetY());
        cam.zoom = 1f / camera.getZoom();
        cam.update();

        AnimationFrame frame = state.getCurrentFrame();
        if (frame != null) {
            float originX = frame.getOriginX();
            float originY = frame.getOriginY();

            // Draw sprite
            SpriteFrame spriteFrame = state.getProject().findSpriteFrame(frame.getSpriteFrameId());
            if (spriteFrame != null) {
                Texture texture = getTexture(spriteFrame.getSourceAssetId());
                if (texture != null) {
                    batch.setProjectionMatrix(cam.combined);
                    batch.begin();
                    float drawX = -originX;
                    float drawY = -originY;
                    if (spriteFrame.getSubRegion() != null) {
                        Rect sr = spriteFrame.getSubRegion();
                        batch.draw(texture, drawX, drawY, sr.width, sr.height,
                            (int) sr.x, (int) (texture.getHeight() - sr.y - sr.height),
                            (int) sr.width, (int) sr.height,
                            false, false);
                    } else {
                        batch.draw(texture, drawX, drawY);
                    }
                    batch.end();
                }
            }

            // Draw collision boxes
            shapes.setProjectionMatrix(cam.combined);
            shapes.begin(ShapeRenderer.ShapeType.Line);
            String selectedBoxId = state.getSelectedCollisionBoxId();
            for (CollisionBox box : frame.getCollisionBoxes()) {
                if (box.getId().equals(selectedBoxId)) {
                    shapes.setColor(1f, 1f, 0f, 1f); // Yellow for selected
                } else {
                    shapes.setColor(1f, 0.3f, 0.3f, 1f); // Red for normal
                }
                shapes.rect(box.getX() - originX, box.getY() - originY, box.getWidth(), box.getHeight());
            }
            shapes.end();

            // Draw origin marker
            shapes.begin(ShapeRenderer.ShapeType.Filled);
            shapes.setColor(1f, 0.92f, 0.23f, 1f); // Yellow
            shapes.circle(0, 0, 4 / camera.getZoom());
            shapes.end();

            // Draw crosshair at origin
            shapes.begin(ShapeRenderer.ShapeType.Line);
            shapes.setColor(1f, 0.92f, 0.23f, 0.5f);
            float lineLen = 20 / camera.getZoom();
            shapes.line(-lineLen, 0, lineLen, 0);
            shapes.line(0, -lineLen, 0, lineLen);
            shapes.end();
        }

        Gdx.gl.glDisable(GL30.GL_SCISSOR_TEST);
        // Reset viewport to full screen for ImGui
        Gdx.gl.glViewport(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
    }

    private Texture getTexture(String sourceAssetId) {
        Map<String, Texture> cache = state.getTextureCache();
        SourceAsset asset = state.getProject().findSourceAsset(sourceAssetId);
        if (asset == null) return null;
        String key = asset.getFilePath();
        Texture tex = cache.get(key);
        if (tex == null) {
            try {
                tex = new Texture(Gdx.files.absolute(key));
                cache.put(key, tex);
            } catch (Exception e) {
                return null;
            }
        }
        return tex;
    }

    public void dispose() {
        for (Texture tex : state.getTextureCache().values()) {
            tex.dispose();
        }
        state.getTextureCache().clear();
        batch.dispose();
        shapes.dispose();
    }
}

/*
 * Copyright (c) 2012, 2014, Oracle and/or its affiliates.
 * All rights reserved. Use is subject to license terms.
 *
 * This file is available and licensed under the following license:
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *  - Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the distribution.
 *  - Neither the name of Oracle Corporation nor the names of its
 *    contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.oracle.javafx.scenebuilder.kit.editor.panel.content.mode;

import com.oracle.javafx.scenebuilder.kit.editor.images.ImageUtils;
import com.oracle.javafx.scenebuilder.kit.editor.panel.content.ContentPanelController;
import com.oracle.javafx.scenebuilder.kit.editor.panel.content.driver.pring.AbstractPring;
import com.oracle.javafx.scenebuilder.kit.editor.selection.ObjectSelectionGroup;
import com.oracle.javafx.scenebuilder.kit.editor.selection.Selection;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMDocument;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMObject;
import java.util.Objects;
import javafx.event.EventHandler;
import javafx.geometry.Point2D;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.input.MouseEvent;

/**
 *
 * 
 */


public class PickModeController extends AbstractModeController {

    private HitNodeChrome hitNodeChrome;
    
    public PickModeController(ContentPanelController contentPanelController) {
        super(contentPanelController);
    }
    
    
    /*
     * AbstractModeController
     */
    
    @Override
    public void willResignActive(AbstractModeController nextModeController) {
        contentPanelController.getGlassLayer().setCursor(Cursor.DEFAULT);
        stopListeningToInputEvents();
        removeHitNodeChrome();
    }

    @Override
    public void didBecomeActive(AbstractModeController previousModeController) {
        assert contentPanelController.getGlassLayer() != null;
        
        updateHitNodeChrome();
        startListeningToInputEvents();
        contentPanelController.getGlassLayer().setCursor(ImageUtils.getCSSCursor());
    }
    
    @Override
    public void editorSelectionDidChange() {
        updateHitNodeChrome();
    }

    @Override
    public void fxomDocumentDidChange(FXOMDocument oldDocument) {
        // Same logic as when the scene graph is changed
        fxomDocumentDidRefreshSceneGraph();
    }

    @Override
    public void fxomDocumentDidRefreshSceneGraph() {
        updateHitNodeChrome();
    }

    @Override
    public void dropTargetDidChange() {
        // Should not be invoked : if drag gesture starts, editor controller
        // will switch to EditModeController.
        assert false;
    }
    
    
    /*
     * Private
     */
    

    private void startListeningToInputEvents() {
        final Node glassLayer = contentPanelController.getGlassLayer();
        assert glassLayer.getOnMousePressed() == null;
        
        glassLayer.setOnMousePressed(mousePressedOnGlassLayerListener);
    }
    
    private void stopListeningToInputEvents() {
        final Node glassLayer = contentPanelController.getGlassLayer();
        glassLayer.setOnMousePressed(null);
    }
    
    private final EventHandler<MouseEvent> mousePressedOnGlassLayerListener
            = new EventHandler<MouseEvent>() {
                @Override
                public void handle(MouseEvent e) {
                    mousePressedOnGlassLayer(e);
                }
            };
    
    
    private void mousePressedOnGlassLayer(MouseEvent e) {
        
        
        final Selection selection 
                = contentPanelController.getEditorController().getSelection();
        final FXOMObject hitObject 
                = contentPanelController.pick(e.getSceneX(), e.getSceneY());
        
        if (hitObject == null) {
            selection.clear();
        } else {
            final Point2D hitPoint
                    = computeHitPoint(hitObject, e.getSceneX(), e.getSceneY());

            if (selection.isSelected(hitObject)) {
                assert selection.getGroup() instanceof ObjectSelectionGroup;
                selection.updateHitObject(hitObject, hitPoint);
            } else {
                selection.select(hitObject, hitPoint);
            }
        }
    }
    
    
    private Point2D computeHitPoint(FXOMObject fxomObject, double hitSceneX, double hitSceneY) {
        
        final FXOMObject nodeObject = fxomObject.getClosestNode();
        assert nodeObject != null; // At least the root is a Node
        assert nodeObject.getSceneGraphObject() instanceof Node;
        final Node sceneGraphNode = (Node) nodeObject.getSceneGraphObject();
        return sceneGraphNode.sceneToLocal(hitSceneX, hitSceneY);
    }
    
    
    
    
    private void updateHitNodeChrome() {
        final Selection selection = contentPanelController.getEditorController().getSelection();
        final HitNodeChrome newChrome;
        
        if ((hitNodeChrome == null) 
                || (hitNodeChrome.getFxomObject() != selection.getHitItem())
                || (Objects.equals(hitNodeChrome.getHitPoint(),selection.getHitPoint()) == false)) {
            if (selection.getHitItem() != null) {
                newChrome = makeHitNodeChrome(selection.getHitItem(), selection.getHitPoint());
            } else {
                newChrome = null;
            }
        } else {
            switch(hitNodeChrome.getState()) {
                default:
                case CLEAN:
                    newChrome = hitNodeChrome;
                    break;
                case NEEDS_RECONCILE:
                    newChrome = hitNodeChrome;
                    hitNodeChrome.reconcile();
                    break;
                case NEEDS_REPLACE:
                    newChrome = makeHitNodeChrome(selection.getHitItem(), selection.getHitPoint());
                    assert newChrome.getState() == HitNodeChrome.State.CLEAN;
                    break;
            }
        }
        
        if (newChrome != hitNodeChrome) {
            final Group rudderLayer = contentPanelController.getRudderLayer();
            if (hitNodeChrome != null) {
                rudderLayer.getChildren().remove(hitNodeChrome.getRootNode());
            }
            hitNodeChrome = newChrome;
            if (hitNodeChrome != null) {
                rudderLayer.getChildren().add(hitNodeChrome.getRootNode());
            }
        } else {
            assert (hitNodeChrome == null) || hitNodeChrome.getState() == AbstractPring.State.CLEAN;
        }
    }
    
    
    private HitNodeChrome makeHitNodeChrome(FXOMObject hitItem, Point2D hitPoint) {
        final HitNodeChrome result;
        
        assert hitItem != null;
        
        result = new HitNodeChrome(contentPanelController, hitItem, hitPoint);
        
        return result;
    }
    
    
    private void removeHitNodeChrome() {
        if (hitNodeChrome != null) {
            final Group rudderLayer = contentPanelController.getRudderLayer();
            rudderLayer.getChildren().remove(hitNodeChrome.getRootNode());
            hitNodeChrome = null;
        }
    }
}

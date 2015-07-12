/*
 * Copyright 2015 Igor Maznitsa.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.igormaznitsa.nbmindmap.gui.mmview;

import com.igormaznitsa.nbmindmap.model.MindMapTopic;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;

import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.Dimension2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;

public final class ElementRoot extends AbstractElement {

  private final Dimension2D leftBlockSize = new Dimension();
  private final Dimension2D rightBlockSize = new Dimension();
  
  public ElementRoot(final MindMapTopic topic) {
    super(topic);
  }

  @Override
  public boolean isMoveable() {
    return false;
  }

  @Override
  public boolean isFocusable() {
    return true;
  }

  @Override
  public boolean isRemovable() {
    return false;
  }

  @Override
  public boolean isCollapsed() {
    return false;
  }

  private Shape makeShape(final Configuration cfg, final float x, final float y) {
    return new RoundRectangle2D.Float(x, y, (float) this.bounds.getWidth(), (float) this.bounds.getHeight(), 10.0f * cfg.getScale(), 10.0f * cfg.getScale());
  }

  @Override
  public void drawComponent(final Graphics2D g, final Configuration cfg) {
    g.setStroke(new BasicStroke(cfg.getScale() * cfg.getElementBorderWidth()));

    final Shape shape = makeShape(cfg, 0f, 0f);

    if (cfg.isDropShadow()) {
      g.setColor(cfg.getShadowColor());
      g.fill(makeShape(cfg, 5.0f * cfg.getScale(), 5.0f * cfg.getScale()));
    }

    g.setColor(cfg.getRootBackgroundColor());
    g.fill(shape);

    g.setColor(cfg.getElementBorderColor());
    g.draw(shape);

    g.setColor(cfg.getRootTextColor());
    this.textBlock.paint(g);
    
//    g.setColor(Color.white);
//    g.drawRect((int) (-this.leftBlockSize.getWidth()), -(int) (this.leftBlockSize.getHeight() - this.bounds.getHeight()) / 2, (int) this.leftBlockSize.getWidth(), (int) this.leftBlockSize.getHeight());
//    g.drawRect((int) this.bounds.getWidth(), -(int) (this.rightBlockSize.getHeight() - this.bounds.getHeight()) / 2, (int) this.rightBlockSize.getWidth(), (int) this.rightBlockSize.getHeight());
  }

  @Override
  public void drawConnector(final Graphics2D g, final Rectangle2D source, final Rectangle2D destination, final boolean leftDirection, final Configuration cfg) {
    g.setStroke(new BasicStroke(cfg.getConnectorWidth() * cfg.getScale()));
    g.setColor(cfg.getConnectorColor());
    
    final Path2D path = new Path2D.Double();
    
    final double startX;
    if (destination.getCenterX()<source.getCenterX()){
      // left
      startX = source.getCenterX()-source.getWidth()/4;
    }else{
      // right
      startX = source.getCenterX()+source.getWidth()/4;
    }
    
    path.moveTo(startX, source.getCenterY());
    path.curveTo(startX, destination.getCenterY(), startX, destination.getCenterY(), destination.getCenterX(), destination.getCenterY());
    
    g.draw(path);
  }

  private double calcTotalChildrenHeight(final double vertInset, final boolean left) {
    double result = 0.0d;
    boolean nonfirst = false;
    for (final MindMapTopic t : this.model.getChildren()) {
      final AbstractCollapsableElement w = (AbstractCollapsableElement) t.getPayload();
      final boolean lft = w.isLeftDirection();
      if ((left && lft) || (!left && !lft)){
        if (nonfirst) {
          result += vertInset;
        }
        else {
          nonfirst = true;
        }
        result += w.getBlockSize().getHeight();
      }
    }
    return result;
  }

  @Override
  public void alignElementAndChildren(Configuration cfg, final boolean leftSide, final double cx, final double cy) {
    final double dx = cx;
    final double dy = cy;
    this.moveTo(dx, dy);

    final int textMargin = Math.round(cfg.getScale() * cfg.getTextMargins());
    this.textBlock.setCoordOffset(textMargin, textMargin);
    
    final double insetVert = cfg.getFirstLevelVerticalInset() * cfg.getScale();
    final double insetHorz = cfg.getFirstLevelHorizontalInset() * cfg.getScale();

    final double leftHeight = calcTotalChildrenHeight(insetVert, true);
    final double rightHeight = calcTotalChildrenHeight(insetVert, false);
    
    if (leftHeight>0.0d){
      final double ddx = dx - insetHorz;
      double ddy = dy - (leftHeight-this.bounds.getHeight())/2;
      for(final MindMapTopic t : this.model.getChildren()){
        final AbstractCollapsableElement c = (AbstractCollapsableElement)t.getPayload();
        if (c.isLeftDirection()){
          c.alignElementAndChildren(cfg, true, ddx - c.getBlockSize().getWidth(), ddy);
          ddy += c.getBlockSize().getHeight() + insetVert;
        }
      }
    }
    
    if (rightHeight>0.0d){
      final double ddx = dx + this.bounds.getWidth() + insetHorz;
      double ddy = dy - (rightHeight - this.bounds.getHeight()) / 2;
      for (final MindMapTopic t : this.model.getChildren()) {
        final AbstractCollapsableElement c = (AbstractCollapsableElement) t.getPayload();
        if (!c.isLeftDirection()) {
          c.alignElementAndChildren(cfg, false, ddx, ddy);
          ddy += c.getBlockSize().getHeight() + insetVert;
        }
      }
    }
  }

  @Override
  public void updateElementBounds(final Graphics2D gfx, final Configuration cfg) {
    super.updateElementBounds(gfx, cfg);
    final float marginOffset = (cfg.getTextMargins() << 1) * cfg.getScale();
    this.bounds.setRect(this.bounds.getX(), this.bounds.getY(), this.bounds.getWidth() + marginOffset, this.bounds.getHeight() + marginOffset);
  }

  public Dimension2D getLeftBlockSize(){
    return this.leftBlockSize;
  }
  
  public Dimension2D getRightBlockSize(){
    return this.rightBlockSize;
  }
  
  @Override
  public Dimension2D calcBlockSize(final Configuration cfg, final Dimension2D size) {
    final float insetV = cfg.getScale() * cfg.getFirstLevelVerticalInset();
    final float insetH = cfg.getScale() * cfg.getFirstLevelHorizontalInset();

    final Dimension2D result = size == null ? new Dimension() : size;

    double leftWidth = 0.0d;
    double leftHeight = 0.0d;
    double rightWidth = 0.0d;
    double rightHeight = 0.0d;

    boolean nonfirstOnLeft = false;
    boolean nonfirstOnRight = false;

    for (final MindMapTopic t : this.model.getChildren()) {
      final ElementLevelFirst w = (ElementLevelFirst) t.getPayload();
      
      w.calcBlockSize(cfg, result);
      
      if (w.isLeftDirection()) {
        leftWidth = Math.max(leftWidth, result.getWidth());
        leftHeight += result.getHeight();
        if (nonfirstOnLeft) {
          leftHeight += insetV;
        }
        else {
          nonfirstOnLeft = true;
        }
      }
      else {
        rightWidth = Math.max(rightWidth, result.getWidth());
        rightHeight += result.getHeight();
        if (nonfirstOnRight) {
          rightHeight += insetV;
        }
        else {
          nonfirstOnRight = true;
        }
      }
    }

    leftWidth += nonfirstOnLeft ? insetH : 0.0d;
    rightWidth += nonfirstOnRight ? insetH : 0.0d;
    
    this.leftBlockSize.setSize(leftWidth, leftHeight);
    this.rightBlockSize.setSize(rightWidth, rightHeight);
    
    result.setSize(leftWidth + rightWidth + this.bounds.getWidth(), Math.max(this.bounds.getHeight(), Math.max(leftHeight, rightHeight)));
    return result;
  }

  @Override
  public boolean hasDirection() {
    return true;
  }

}
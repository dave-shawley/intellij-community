/*
 * Copyright 2000-2013 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.xdebugger.impl.breakpoints;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.components.ComponentSerializationUtil;
import com.intellij.openapi.editor.markup.GutterDraggableObject;
import com.intellij.openapi.editor.markup.GutterIconRenderer;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.UserDataHolderBase;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.pom.Navigatable;
import com.intellij.util.StringBuilderSpinAllocator;
import com.intellij.util.xmlb.SkipDefaultValuesSerializationFilters;
import com.intellij.util.xmlb.XmlSerializer;
import com.intellij.xdebugger.XDebugSession;
import com.intellij.xdebugger.XDebuggerBundle;
import com.intellij.xdebugger.XSourcePosition;
import com.intellij.xdebugger.breakpoints.SuspendPolicy;
import com.intellij.xdebugger.breakpoints.XBreakpoint;
import com.intellij.xdebugger.breakpoints.XBreakpointProperties;
import com.intellij.xdebugger.breakpoints.XBreakpointType;
import com.intellij.xdebugger.impl.DebuggerSupport;
import com.intellij.xdebugger.impl.XDebugSessionImpl;
import com.intellij.xdebugger.impl.XDebuggerSupport;
import com.intellij.xdebugger.impl.actions.EditBreakpointAction;
import org.jdom.Element;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Collections;
import java.util.List;

/**
 * @author nik
 */
public class XBreakpointBase<Self extends XBreakpoint<P>, P extends XBreakpointProperties, S extends BreakpointState> extends UserDataHolderBase implements XBreakpoint<P>, Comparable<Self> {
  private static final SkipDefaultValuesSerializationFilters SERIALIZATION_FILTERS = new SkipDefaultValuesSerializationFilters();
  @NonNls private static final String BR_NBSP = "<br>&nbsp;";
  private final XBreakpointType<Self, P> myType;
  private final @Nullable P myProperties;
  protected final S myState;
  private final XBreakpointManagerImpl myBreakpointManager;
  private Icon myIcon;
  private CustomizedBreakpointPresentation myCustomizedPresentation;

  public XBreakpointBase(final XBreakpointType<Self, P> type, XBreakpointManagerImpl breakpointManager, final @Nullable P properties, final S state) {
    myState = state;
    myType = type;
    myProperties = properties;
    myBreakpointManager = breakpointManager;
  }

  protected XBreakpointBase(final XBreakpointType<Self, P> type, XBreakpointManagerImpl breakpointManager, S breakpointState) {
    myState = breakpointState;
    myType = type;
    myBreakpointManager = breakpointManager;
    myProperties = type.createProperties();
    if (myProperties != null) {
      ComponentSerializationUtil.loadComponentState(myProperties, myState.getPropertiesElement());
    }
  }

  public final Project getProject() {
    return myBreakpointManager.getProject();
  }

  protected XBreakpointManagerImpl getBreakpointManager() {
    return myBreakpointManager;
  }

  public final void fireBreakpointChanged() {
    clearIcon();
    myBreakpointManager.fireBreakpointChanged(this);
  }

  @Override
  public XSourcePosition getSourcePosition() {
    return getType().getSourcePosition(this);
  }

  @Override
  public Navigatable getNavigatable() {
    XSourcePosition position = getSourcePosition();
    if (position == null) {
      return null;
    }
    return position.createNavigatable(getProject());
  }

  @Override
  public boolean isEnabled() {
    return myState.isEnabled();
  }

  @Override
  public void setEnabled(final boolean enabled) {
    if (enabled != isEnabled()) {
      myState.setEnabled(enabled);
      fireBreakpointChanged();
    }
  }

  @Override
  @NotNull
  public SuspendPolicy getSuspendPolicy() {
    return myState.getSuspendPolicy();
  }

  @Override
  public void setSuspendPolicy(@NotNull SuspendPolicy policy) {
    if (myState.getSuspendPolicy() != policy) {
      myState.setSuspendPolicy(policy);
      fireBreakpointChanged();
    }
  }

  @Override
  public boolean isLogMessage() {
    return myState.isLogMessage();
  }

  @Override
  public void setLogMessage(final boolean logMessage) {
    if (logMessage != isLogMessage()) {
      myState.setLogMessage(logMessage);
      fireBreakpointChanged();
    }
  }

  @Override
  public String getLogExpression() {
    return myState.getLogExpression();
  }

  @Override
  public void setLogExpression(@Nullable final String expression) {
    if (!Comparing.equal(getLogExpression(), expression)) {
      myState.setLogExpression(expression);
      fireBreakpointChanged();
    }
  }

  @Override
  public String getCondition() {
    return myState.getCondition();
  }

  @Override
  public void setCondition(@Nullable final String condition) {
    if (!Comparing.equal(condition, getCondition())) {
      myState.setCondition(condition);
      fireBreakpointChanged();
    }
  }

  @Override
  public long getTimeStamp() {
    return myState.getTimeStamp();
  }

  public boolean isValid() {
    return true;
  }

  @Override
  @Nullable
  public P getProperties() {
    return myProperties;
  }

  @Override
  @NotNull
  public XBreakpointType<Self,P> getType() {
    return myType;
  }

  public S getState() {
    Element propertiesElement = myProperties != null ? XmlSerializer.serialize(myProperties.getState(), SERIALIZATION_FILTERS) : null;
    myState.setPropertiesElement(propertiesElement);
    return myState;
  }

  public XBreakpointDependencyState getDependencyState() {
    return myState.getDependencyState();
  }

  public void setDependencyState(XBreakpointDependencyState state) {
    myState.setDependencyState(state);
  }

  public String getGroup() {
    return myState.getGroup();
  }

  public void setGroup(String group) {
    group = StringUtil.nullize(group);
    myState.setGroup(group);
  }

  public void dispose() {
  }

  @Override
  public String toString() {
    return "XBreakpointBase(type=" + myType + ")";
  }

  @Nullable
  protected GutterDraggableObject createBreakpointDraggableObject() {
    return null;
  }

  protected List<? extends AnAction> getAdditionalPopupMenuActions(XDebugSession session) {
    return Collections.emptyList();
  }

  public String getDescription() {
    @NonNls StringBuilder builder = StringBuilderSpinAllocator.alloc();
    try {
      builder.append("<html><body>");
      builder.append(XBreakpointUtil.getDisplayText(this));

      String errorMessage = getErrorMessage();
      if (errorMessage != null) {
        builder.append(BR_NBSP);
        builder.append("<font color=\"red\">");
        builder.append(errorMessage);
        builder.append("</font>");
      }

      SuspendPolicy suspendPolicy = getSuspendPolicy();
      if (suspendPolicy == SuspendPolicy.THREAD) {
        builder.append(BR_NBSP).append(XDebuggerBundle.message("xbreakpoint.tooltip.suspend.policy.thread"));
      }
      else if (suspendPolicy == SuspendPolicy.NONE) {
        builder.append(BR_NBSP).append(XDebuggerBundle.message("xbreakpoint.tooltip.suspend.policy.none"));
      }

      String condition = getCondition();
      if (condition != null) {
        builder.append(BR_NBSP);
        builder.append(XDebuggerBundle.message("xbreakpoint.tooltip.condition"));
        builder.append("&nbsp;");
        builder.append(condition);
      }

      if (isLogMessage()) {
        builder.append(BR_NBSP).append(XDebuggerBundle.message("xbreakpoint.tooltip.log.message"));
      }
      String logExpression = getLogExpression();
      if (logExpression != null) {
        builder.append(BR_NBSP);
        builder.append(XDebuggerBundle.message("xbreakpoint.tooltip.log.expression"));
        builder.append("&nbsp;");
        builder.append(logExpression);
      }

      XBreakpoint<?> masterBreakpoint = getBreakpointManager().getDependentBreakpointManager().getMasterBreakpoint(this);
      if (masterBreakpoint != null) {
        builder.append(BR_NBSP);
        String str = XDebuggerBundle.message("xbreakpoint.tooltip.depends.on");
        builder.append(str);
        builder.append("&nbsp;");
        builder.append(XBreakpointUtil.getDisplayText(masterBreakpoint));
      }

      builder.append("</body><html");
      return builder.toString();
    }
    finally {
      StringBuilderSpinAllocator.dispose(builder);
    }
  }

  protected void updateIcon() {
    final Icon icon = calculateSpecialIcon();
    myIcon = icon != null ? icon : getType().getEnabledIcon();
  }

  protected void setIcon(Icon icon) {
    myIcon = icon;
  }

  @Nullable
  protected final Icon calculateSpecialIcon() {
    if (!isEnabled()) {
      // disabled icon takes precedence to other to visually distinguish it and provide feedback then it is enabled/disabled
      // (e.g. in case of mute-mode we would like to differentiate muted but enabled breakpoints from simply disabled ones)
      return getType().getDisabledIcon();
    }

    XDebugSessionImpl session = getBreakpointManager().getDebuggerManager().getCurrentSession();
    if (session == null) {
      if (getBreakpointManager().getDependentBreakpointManager().getMasterBreakpoint(this) != null) {
        return getType().getInactiveDependentIcon();
      }
    }
    else {
      if (session.areBreakpointsMuted()) {
        return AllIcons.Debugger.Db_muted_breakpoint;
      }
      if (session.isInactiveSlaveBreakpoint(this)) {
        return getType().getInactiveDependentIcon();
      }
      CustomizedBreakpointPresentation presentation = session.getBreakpointPresentation(this);
      if (presentation != null) {
        Icon icon = presentation.getIcon();
        if (icon != null) {
          return icon;
        }
      }
    }
    if (myCustomizedPresentation != null) {
      final Icon icon = myCustomizedPresentation.getIcon();
      if (icon != null) {
        return icon;
      }
    }
    return null;
  }

  public Icon getIcon() {
    if (myIcon == null) {
      updateIcon();
    }
    return myIcon;
  }

  @Nullable
  public String getErrorMessage() {
    final XDebugSessionImpl currentSession = getBreakpointManager().getDebuggerManager().getCurrentSession();
    if (currentSession != null) {
      CustomizedBreakpointPresentation presentation = currentSession.getBreakpointPresentation(this);
      if (presentation != null) {
        final String message = presentation.getErrorMessage();
        if (message != null) return message;
      }
    }
    return myCustomizedPresentation != null ? myCustomizedPresentation.getErrorMessage() : null;
  }

  CustomizedBreakpointPresentation getCustomizedPresentation() {
    return myCustomizedPresentation;
  }

  public void setCustomizedPresentation(CustomizedBreakpointPresentation presentation) {
    myCustomizedPresentation = presentation;
  }

  @NotNull
  public GutterIconRenderer createGutterIconRenderer() {
    return new BreakpointGutterIconRenderer();
  }

  public void clearIcon() {
    myIcon = null;
  }

  @Override
  public int compareTo(@NotNull Self self) {
    return myType.getBreakpointComparator().compare((Self)this, self);
  }

  protected class BreakpointGutterIconRenderer extends GutterIconRenderer {
    @Override
    @NotNull
    public Icon getIcon() {
      return XBreakpointBase.this.getIcon();
    }

    @Override
    @Nullable
    public AnAction getClickAction() {
      return new RemoveBreakpointGutterIconAction(XBreakpointBase.this);
    }

    @Override
    @Nullable
    public AnAction getMiddleButtonClickAction() {
      return new ToggleBreakpointGutterIconAction(XBreakpointBase.this);
    }

    @Nullable
    @Override
    public AnAction getRightButtonClickAction() {
      return new EditBreakpointAction.ContextAction(this, XBreakpointBase.this, DebuggerSupport.getDebuggerSupport(XDebuggerSupport.class));
    }

    @Override
    public Alignment getAlignment() {
      return Alignment.RIGHT;
    }

    @Override
    @Nullable
    public ActionGroup getPopupMenuActions() {
      return null;
    }

    @Override
    @Nullable
    public String getTooltipText() {
      return getDescription();
    }

    @Override
    public GutterDraggableObject getDraggableObject() {
      return createBreakpointDraggableObject();
    }

    private XBreakpointBase<?,?,?> getBreakpoint() {
      return XBreakpointBase.this;
    }
    @Override
    public boolean equals(Object obj) {
      return obj instanceof XLineBreakpointImpl.BreakpointGutterIconRenderer
             && getBreakpoint() == ((XLineBreakpointImpl.BreakpointGutterIconRenderer)obj).getBreakpoint()
             && Comparing.equal(getIcon(), ((XLineBreakpointImpl.BreakpointGutterIconRenderer)obj).getIcon());
    }

    @Override
    public int hashCode() {
      return getBreakpoint().hashCode();
    }
  }
}

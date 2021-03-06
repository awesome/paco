/*
* Copyright 2011 Google Inc. All Rights Reserved.
* 
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance  with the License.  
* You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an
* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
* KIND, either express or implied.  See the License for the
* specific language governing permissions and limitations
* under the License.
*/
package com.google.sampling.experiential.client;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.DisclosureEvent;
import com.google.gwt.user.client.ui.DisclosureHandler;
import com.google.gwt.user.client.ui.DisclosurePanel;
import com.google.gwt.user.client.ui.DisclosurePanelImages;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.sampling.experiential.shared.ExperimentDAO;
import com.google.sampling.experiential.shared.FeedbackDAO;
import com.google.sampling.experiential.shared.LoginInfo;

/**
 * The main panel for viewing the details of an experiment
 * Also used as the basis of creation and editing of experiments.
 * Delegates specific parts of experiment definition to sub panels.
 * Handles communication with subpanels about state of edits.
 * 
 * @author Bob Evans
 *
 */
public class ExperimentDefinitionPanel extends Composite {

  private ExperimentDAO experiment;
  private ArrayList<ExperimentListener> listeners;
  private boolean admin;

  private VerticalPanel formPanel;

  private HashMap<String, Widget> fieldToWidgetMap;
  private int keyValueCounter = 0;
  private ContentTypeView contentTypeView;
  private CheckBox publishCheckBox;
  private TextArea adminList;
  private DisclosurePanel publishedUsersPanel;
  private TextArea userList;
  private LoginInfo loginInfo;
  private CheckBox customFeedbackCheckBox;
  private DisclosurePanel customFeedbackPanel;
  private TextArea customFeedbackText;



  public ExperimentDefinitionPanel(
      ExperimentDAO experiment, boolean admin, LoginInfo loginInfo, ExperimentListener listener) {
    this.experiment = experiment;
    this.admin = admin;
    this.loginInfo = loginInfo;
    this.listeners = new ArrayList<ExperimentListener>();
    if (listener != null) {
      listeners.add(listener);
    }
    formPanel = new VerticalPanel();
    initWidget(formPanel);

    String titleText = "Experiment Definition";
    if (!isAdmin()) {
      titleText = "Joined " + titleText;
      titleText += " NOT EDITABLE";
    }
    Label lblExperimentDefinition = new Label(titleText);
    lblExperimentDefinition.setStyleName("paco-HTML-Large");
    formPanel.add(lblExperimentDefinition);

    createExperimentForm();

  }


  private boolean isAdmin() {
    return admin;
  }

  protected void fireCanceled() {
    fireExperimentCode(ExperimentListener.CANCELED);
  }

  protected void saveExperiment() {
    fireExperimentCode(ExperimentListener.SAVED);
  }

  private void fireExperimentCode(int code) {
    for (ExperimentListener listener : listeners) {
      listener.eventFired(code, experiment, false);
    }
  }

  private void createExperimentForm() {
    fieldToWidgetMap = Maps.newHashMap();
    keyValueCounter = 0;
    formPanel.add(createTitlePanel(experiment));

    formPanel.add(createDescriptionPanel(experiment));
    formPanel.add(createCreatorPanel(experiment));

    if (isAdmin()) {
      formPanel.add(createAdminDisclosurePanel(experiment));
    }

    formPanel.add(createInformedConsentPanel(experiment));

    formPanel.add(createDurationPanel(experiment));

    createSchedulePanel(experiment);

    formPanel.add(createContentTypeView("questionsChange", experiment.getQuestionsChange()));

    formPanel.add(createInputsHeader());
    formPanel.add(createInputsListPanel(experiment));
    createFeedbackEntryPanel(experiment);
    if (isAdmin()) {
      createPublishingPanel(experiment);
      createButtonPanel(experiment);
    }

  }

  /**
   * @param experiment2
   * @return
   */
  private Widget createFeedbackEntryPanel(ExperimentDAO experiment2) {
    // checkbox for default or custom feedback "[] Create Custom Feedback Page"
    // if custom selected then fill with feedback from experiment in TextArea
    HorizontalPanel feedbackPanel = new HorizontalPanel();
    customFeedbackCheckBox = new CheckBox();
    customFeedbackCheckBox.setChecked(experiment.getFeedback() != null && 
        experiment.getFeedback().length > 0 && 
        !defaultFeedback(experiment.getFeedback()[0]));
    feedbackPanel.add(customFeedbackCheckBox);
    Label feedbackLabel = new Label("Custom Feedback");
    feedbackPanel.add(feedbackLabel);
    formPanel.add(feedbackPanel);

    createCustomFeedbackDisclosurePanel(experiment);
    formPanel.add(customFeedbackPanel);
    return feedbackPanel;
  }


  /**
   * @param experiment2
   */
  private void createCustomFeedbackDisclosurePanel(ExperimentDAO experiment2) {
    customFeedbackPanel = new DisclosurePanel();

    final DisclosurePanelHeader closedHeaderWidget = new DisclosurePanelHeader(false, 
        "<b>Click to edit custom feedback</b>");
    final DisclosurePanelHeader openHeaderWidget = new DisclosurePanelHeader(true, 
        "<b>Click to close editing of custom feedback</b>");

    customFeedbackPanel.setHeader(closedHeaderWidget);
    customFeedbackPanel.addEventHandler(new DisclosureHandler() {
      public void onClose(DisclosureEvent event) {
        customFeedbackPanel.setHeader(closedHeaderWidget);
      }

      public void onOpen(DisclosureEvent event) {
        customFeedbackPanel.setHeader(openHeaderWidget);
      }
    });

    VerticalPanel userContentPanel = new VerticalPanel();
    Label instructionLabel =
        new Label("Enter custom feedback page html and javascript");
    userContentPanel.add(instructionLabel);

    customFeedbackText = new TextArea();
    customFeedbackText.setCharacterWidth(100);
    customFeedbackText.setHeight("100");

    FeedbackDAO[] feedbacks = experiment.getFeedback();
    
    if (feedbacks != null && feedbacks.length > 0 && !defaultFeedback(feedbacks[0])) {
      customFeedbackText.setText(feedbacks[0].getText());
    }

    userContentPanel.add(customFeedbackText);
    customFeedbackPanel.setContent(userContentPanel);
  }


  /**
   * @param feedbackDAO
   * @return
   */
  private boolean defaultFeedback(FeedbackDAO feedbackDAO) {
    return feedbackDAO.getFeedbackType().equals(FeedbackDAO.DISPLAY_FEEBACK_TYPE) &&
    feedbackDAO.getText().equals(FeedbackDAO.DEFAULT_FEEDBACK_MSG);
  }


  private VerticalPanel createTitlePanel(ExperimentDAO experiment) {
    return createFormLine("Title", experiment.getTitle());
  }

  private VerticalPanel createDescriptionPanel(ExperimentDAO experiment) {
    return createFormArea("Description (<500 chars)", experiment.getDescription(), 75, "100");
  }

  private VerticalPanel createCreatorPanel(ExperimentDAO experiment) {
    return createDisplayLine("Creator",
        experiment.getCreator() != null ? experiment.getCreator() : loginInfo.getEmailAddress());
  }

  private VerticalPanel createInformedConsentPanel(ExperimentDAO experiment) {
    return createFormArea(
        "Informed Consent Text", experiment.getInformedConsentForm(), 100, "200");
  }

  private HTML createInputsHeader() {
    HTML questionsPrompt = new HTML("<h2>Enter at least one question</h2>");
    questionsPrompt.setStyleName("keyLabel");
    return questionsPrompt;
  }

  private DurationView createDurationPanel(ExperimentDAO experiment) {
    DurationView durationPanel = new DurationView(
        experiment.getFixedDuration(), experiment.getStartDate(), experiment.getEndDate());
    fieldToWidgetMap.put("duration", durationPanel);
    return durationPanel;
  }

  private InputsListPanel createInputsListPanel(ExperimentDAO experiment) {
    InputsListPanel inputsListPanel = new InputsListPanel(experiment);
    inputsListPanel.setStyleName("left");
    return inputsListPanel;
  }

  private void createButtonPanel(ExperimentDAO experiment) {
    HorizontalPanel buttonPanel = new HorizontalPanel();
    if (isAdmin()) {
      buttonPanel.add(createSubmitButton(experiment));
    }
    buttonPanel.add(createCancelButton());
    formPanel.add(buttonPanel);
  }

  private void createPublishingPanel(ExperimentDAO experiment) {
    HorizontalPanel publishingPanel = new HorizontalPanel();
    publishCheckBox = new CheckBox();
    publishCheckBox.setValue(experiment.getPublished());
    publishingPanel.add(publishCheckBox);
    Label publishLabel = new Label("Published");
    publishingPanel.add(publishLabel);
    formPanel.add(publishingPanel);

    createPublishedUsersDisclosurePanel(experiment);
    formPanel.add(publishedUsersPanel);
  }

  private void createSchedulePanel(ExperimentDAO experiment) {
    SchedulePanel sp = new SchedulePanel(experiment.getSchedule());
    formPanel.add(sp);
  }

  private DisclosurePanel createAdminDisclosurePanel(ExperimentDAO experiment) {
    final DisclosurePanel adminPanel = new DisclosurePanel();
    final DisclosurePanelHeader closedHeaderWidget =
        new DisclosurePanelHeader(false, "<b>Click to edit administrators</b>");
    final DisclosurePanelHeader openHeaderWidget =
        new DisclosurePanelHeader(true, "<b>Click to close editing of administrators</b>");
    adminPanel.setHeader(closedHeaderWidget);
    adminPanel.addEventHandler(new DisclosureHandler() {
      public void onClose(DisclosureEvent event) {
        adminPanel.setHeader(closedHeaderWidget);
      }

      public void onOpen(DisclosureEvent event) {
        adminPanel.setHeader(openHeaderWidget);
      }
    });
    VerticalPanel adminContentPanel = new VerticalPanel();
    Label instructionlabel = createLabel(
        "Enter emails separated by commas of who can edit this experiment and see results");
    adminContentPanel.add(instructionlabel);

    adminList = new TextArea();
    adminList.setCharacterWidth(100);
    adminList.setHeight("100");
    String[] adminStrArray = experiment.getAdmins();
    List<String> admins = Lists.newArrayList(adminStrArray);
    if (!admins.contains(loginInfo.getEmailAddress())) {
      admins.add(loginInfo.getEmailAddress());
    }
    adminList.setText(toCSVString(admins));

    adminContentPanel.add(adminList);
    adminPanel.setContent(adminContentPanel);
    return adminPanel;
  }



  final DisclosurePanelImages images =
      (DisclosurePanelImages) GWT.create(DisclosurePanelImages.class);

  class DisclosurePanelHeader extends HorizontalPanel {
    public DisclosurePanelHeader(boolean isOpen, String html) {
      add(
          isOpen ? images.disclosurePanelOpen().createImage()
              : images.disclosurePanelClosed().createImage());
      add(new HTML(html));
    }
  }

  private void createPublishedUsersDisclosurePanel(ExperimentDAO experiment) {
    publishedUsersPanel = new DisclosurePanel();

    final DisclosurePanelHeader closedHeaderWidget =
        new DisclosurePanelHeader(false, "<b>Click to edit published audience</b>");
    final DisclosurePanelHeader openHeaderWidget =
        new DisclosurePanelHeader(true, "<b>Click to close editing of published audience</b>");

    publishedUsersPanel.setHeader(closedHeaderWidget);
    publishedUsersPanel.addEventHandler(new DisclosureHandler() {

      public void onClose(DisclosureEvent event) {
        publishedUsersPanel.setHeader(closedHeaderWidget);
      }

      public void onOpen(DisclosureEvent event) {
        publishedUsersPanel.setHeader(openHeaderWidget);
      }
    });

    VerticalPanel userContentPanel = new VerticalPanel();
    Label instructionLabel =
        new Label("Enter emails separated by commas. An empty list is public.");
    userContentPanel.add(instructionLabel);

    userList = new TextArea();
    userList.setCharacterWidth(100);
    userList.setHeight("100");

    String[] usersStrArray = experiment.getPublishedUsers();
    List<String> userEmails = Lists.newArrayList(usersStrArray);
    userList.setText(toCSVString(userEmails));

    userContentPanel.add(userList);
    publishedUsersPanel.setContent(userContentPanel);
  }

  private VerticalPanel createFormLine(String key, String value) {
    VerticalPanel line = new VerticalPanel();
    line.setStyleName("left");
    Label keyLabel = new Label(key + ": ");
    keyLabel.setStyleName("keyLabel");
    TextBox valueBox = new TextBox();
    if (value != null) {
      valueBox.setText(value);
    }
    valueBox.setEnabled(isAdmin());
    line.add(keyLabel);
    line.add(valueBox);
    fieldToWidgetMap.put(key, valueBox);
    return line;
  }

  private VerticalPanel createFormArea(String key, String value, int width, String height) {
    VerticalPanel line = new VerticalPanel();
    line.setStyleName("left");
    Label keyLabel = new Label(key + ": ");
    keyLabel.setStyleName("keyLabel");
    TextArea valueBox = new TextArea();
    valueBox.setCharacterWidth(width);
    valueBox.setHeight(height);
    if (value != null) {
      valueBox.setText(value);
    }
    valueBox.setEnabled(isAdmin());
    line.add(keyLabel);
    line.add(valueBox);
    fieldToWidgetMap.put(key, valueBox);
    return line;
  }

  private VerticalPanel createDisplayLine(String key, String value) {
    VerticalPanel line = new VerticalPanel();
    line.setStyleName("left");
    Label keyLabel = new Label(key + ": ");
    keyLabel.setStyleName("keyLabel");
    Label valueBox = new Label();
    if (value != null) {
      valueBox.setText(value);
    }
    line.add(keyLabel);
    line.add(valueBox);
    fieldToWidgetMap.put(key, valueBox);
    return line;
  }


  private ContentTypeView createContentTypeView(String key, boolean value) {
    contentTypeView = new ContentTypeView(value);
    fieldToWidgetMap.put(key, contentTypeView);
    return contentTypeView;
  }

  private VerticalPanel createFormLine(String key) {
    return createFormLine(key, null);
  }

  private void resetDetailPanelAndDisplay() {
    formPanel.clear();
    keyValueCounter = 0;
  }

  private String toCSVString(List<String> list) {
    StringBuilder buf = new StringBuilder();
    boolean first = true;
    for (String item : list) {
      if (first) {
        first = false;
      } else {
        buf.append(", ");
      }
      buf.append(item);
    }
    return buf.toString();
  }

  private Label createLabel(String title) {
    Label responseTypeLabel = new Label(title);
    responseTypeLabel.setStyleName("keyLabel");
    return responseTypeLabel;
  }

  /**
   * @return
   */
  private Widget createCancelButton() {
    Button cancelButton = new Button("Cancel");
    cancelButton.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        fireCanceled();
      }
    });
    return cancelButton;
  }

  private Widget createSubmitButton(final ExperimentDAO experiment) {

    Button whatButton =
        new Button(experiment.getId() == null ? "Create Experiment" : "Update Experiment");
    whatButton.addClickListener(new ClickListener() {

      @Override
      public void onClick(Widget sender) {
        submitEvent(experiment);
      }

    });
    return whatButton;
  }

  private void submitEvent(ExperimentDAO experiment) {
    try {
      setTitleOn(experiment);
      setDescriptionOn(experiment);
      setCreatorOn(experiment);
      setAdminsOn(experiment);
      setInformedConsentOn(experiment);
      setQuestionsChangeOn(experiment);
      setDurationOn(experiment);
      setFeedbackOn(experiment);
      setPublishingOn(experiment);
      setModifyDateOn(experiment);

      saveExperiment();
    } catch (Throwable t) {
      Window.alert("Throwable: " + t.getMessage());
    }
  }

  private void setCreatorOn(ExperimentDAO experiment) {
    if (experiment.getCreator() == null) {
      experiment.setCreator(loginInfo.getEmailAddress());
    }
  }

  private void setDescriptionOn(ExperimentDAO experiment) {
    experiment.setDescription(((TextArea)fieldToWidgetMap.get(
        "Description (<500 chars)")).getText());
  }

  private void setTitleOn(ExperimentDAO experiment) {
    experiment.setTitle(((TextBox) fieldToWidgetMap.get("Title")).getText());
  }

  private void setQuestionsChangeOn(ExperimentDAO experiment) {
    experiment.setQuestionsChange(
        ((BooleanValueHolder) fieldToWidgetMap.get("questionsChange")).getValue());
  }

  private void setInformedConsentOn(ExperimentDAO experiment) {
    experiment.setInformedConsentForm(
        ((TextArea) fieldToWidgetMap.get("Informed Consent Text")).getText());
  }

  private void setModifyDateOn(ExperimentDAO experiment) {
    if (experiment.getModifyDate() == null) {
      experiment.setModifyDate(new Date().getTime());
    }
  }



  private void setPublishingOn(ExperimentDAO experiment) {
    experiment.setPublished(publishCheckBox.getValue());
    setPublishedUsersOn(experiment);
  }

  private void setFeedbackOn(ExperimentDAO experiment) {
    if (!customFeedbackCheckBox.getValue()) {
      experiment.setFeedback(new FeedbackDAO[] {new FeedbackDAO(null, 
          FeedbackDAO.DISPLAY_FEEBACK_TYPE, FeedbackDAO.DEFAULT_FEEDBACK_MSG)});
    } else {
      experiment.setFeedback(new FeedbackDAO[] {new FeedbackDAO(null, 
          FeedbackDAO.DISPLAY_FEEBACK_TYPE, customFeedbackText.getText())});
    }
  }

  private void setDurationOn(ExperimentDAO experiment) {
    DurationView durationView = (DurationView) fieldToWidgetMap.get("duration");
    experiment.setFixedDuration(durationView.isFixedDuration());
    if (experiment.getFixedDuration()) {
      experiment
          .setStartDate(durationView.getStartDate() != null ? Long.valueOf(
              durationView.getStartDate().getTime()) : null);
      experiment
          .setEndDate(durationView.getEndDate() != null
              ? Long.valueOf(durationView.getEndDate().getTime()) : null);
    } else {
      experiment.setStartDate(null);
      experiment.setEndDate(null);
    }
  }

  private void setAdminsOn(ExperimentDAO experiment) {
    List<String> admins = new ArrayList<String>();
    String adminsText = adminList.getText();
    if (adminsText.length() == 0) {
      admins.add(loginInfo.getEmailAddress());
    } else {
      Splitter sp = Splitter.on(",").trimResults().omitEmptyStrings();
      for (String admin : sp.split(adminsText)) {
        admins.add(admin);
      }
    }
    String[] adminStrArray = new String[admins.size()];
    adminStrArray = admins.toArray(adminStrArray);
    experiment.setAdmins(adminStrArray);
  }

  private void setPublishedUsersOn(ExperimentDAO experiment) {
    List<String> userEmails = new ArrayList<String>();
    String userListText = userList.getText();
    if (userListText.length() > 0) {
      Splitter sp = Splitter.on(",").trimResults().omitEmptyStrings();
      for (String userEmail : sp.split(userListText)) {
        userEmails.add(userEmail);
      }
    }
    String[] userEmailsStrArray = new String[userEmails.size()];
    userEmailsStrArray = userEmails.toArray(userEmailsStrArray);
    experiment.setPublishedUsers(userEmailsStrArray);
  }
}

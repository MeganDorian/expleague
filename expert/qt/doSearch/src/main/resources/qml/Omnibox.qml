import QtQuick 2.5
import QtQuick.Controls 1.4
import QtQuick.Controls.Styles 1.4
import QtQuick.Layouts 1.1
import QtQuick.Window 2.0

import ExpLeague 1.0

import "."

Rectangle {
    id: self
    color: Palette.backgroundColor

    property alias text: input.text
    property Window window
    property Item completion
    property NavigationManager navigation
    property var commit: (function(tab){})
    property string pageSearch: ""

    function select(type) {
        if (type === "page") {
            selector.currentIndex = 2
        }
        else if (type === "site") {
            selector.currentIndex = 1
        }
        else {
            selector.currentIndex = 0
        }
    }

    onFocusChanged: {
        if (focus) {
            input.focus = true
        }
    }
    RowLayout {
        anchors.fill: self
        anchors.margins: 1
        spacing: 0
        ComboBox {
            id: selector
            Layout.fillHeight: true
            Layout.preferredWidth: 75
            Layout.rightMargin: -4
            textRole: "name"
            model: ListModel {
                ListElement {
                    name: qsTr("интернет: ")
                }
                ListElement {
                    name: qsTr("сайт: ")
                }
                ListElement {
                    name: qsTr("страница: ")
                }
            }
            style: ComboBoxStyle {
                background: Rectangle {
                    anchors.fill: parent
                    color: Palette.selectedColor
                    border.color: "darkgray"
                    border.width: 1
                    layer.mipmap: true
                    radius: 4
                }
                label: Text {
                    verticalAlignment: Text.AlignVCenter
                    horizontalAlignment: Text.AlignRight
                    text: control.currentText
                    color: Palette.selectedTextColor
                }
            }
        }

        TextField {
            Layout.fillHeight: true
            Layout.fillWidth: true
            id: input
            text: navigation.context ? navigation.context.lastRequest.query : ""
            selectByMouse: true
            inputMethodHints: Qt.ImhNoPredictiveText
            style: TextFieldStyle{
                renderType: Text.NativeRendering
            }
            onTextChanged: {
                if (!focus)
                    return
                if (text.length > 2 && completion) {
                    completion.textField = this
                    completion.textToSugget = text
                    completion.visible = true
                }
                else if (completion) {
                    completion.visible = false
                }
            }
            onFocusChanged: {
                if (!focus) {
                    if (completion && !completion.list.focus)
                        completion.visible = false
                    return
                }

                if (!completion || !completion.visible) {
                    selectAll()
                }
                else if (completion) { // reload completion menu
                    var text = input.text
                    input.text = ""
                    input.text = text
                }
            }

            Keys.enabled: true
            Keys.onPressed: {
                if (!focus)
                    return

                if (event.key === Qt.Key_Enter || event.key === Qt.Key_Return) {
                    focus = false
                    self.commit((event.modifiers & (Qt.ControlModifier | Qt.MetaModifier)) != 0)
                    self.visible = false
                    if (completion)
                        completion.visible = false
                    event.accepted = true
                }
                else if (event.key === Qt.Key_Down && completion && completion.visible) {
                    completion.list.forceActiveFocus()
                    event.accepted = true
                }
                else if (event.key === Qt.Key_Escape) {
                    self.visible = false
                    completion.visible = false
                    event.accepted = true
                }
            }
            Connections {
                target: suggest
                onItemChoosen: {
                    text = suggestion
                    self.commit(false)
                    self.visible = false
                }
            }
        }
    }

    states: [
        State {
            name: "internet"
            when: selector.currentIndex == 0
            PropertyChanges {
                target: self
                commit: (function (tab) {
                    navigation.handleOmnibox(input.text, tab)
                    input.text = Qt.binding(function() {return navigation.context.lastRequest.query})
                })
                completion: suggest
                text: navigation.context ? navigation.context.lastRequest.query : ""
            }
        },
        State {
            name: "site"
            when: selector.currentIndex == 1
            PropertyChanges {
                target: self
                commit: (function (tab) {
                    navigation.handleOmnibox("#site " + input.text, false)
                    input.text = Qt.binding(function() {return navigation.context.lastRequest.query})
                })
                completion: suggest
                text: navigation.context ? navigation.context.lastRequest.query : ""
            }
        },
        State {
            name: "page"
            when: selector.currentIndex == 2
            PropertyChanges {
                target: self
                commit: (function (tab) {
                    pageSearch = input.text
                    if (typeof navigation.activeScreen.pageSearch !== "undefined") {
                        navigation.activeScreen.pageSearch = pageSearch
                        navigation.activeScreen.forceActiveFocus()
                    }
//                    navigation.handleOmnibox("#page " + pageSearch, false)
                    input.text = Qt.binding(function() {return pageSearch})
                })
                completion: null
                text: pageSearch
            }
        }
    ]
}

import QtQuick 2.7
import QtQuick.Layouts 1.1
import QtQuick.Controls 1.4
import QtWebEngine 1.3
import QtQuick.Window 2.0

import ExpLeague 1.0

import "."

Item {
    property var context: dosearch.navigation.context
    id: self
    property WebEngineView webView: google.visible ? google : yandex
    anchors.fill: parent

    RowLayout {
        anchors.fill: parent
        Item {Layout.preferredWidth: 1}

        Item {
            Layout.minimumWidth: 33
            Layout.maximumWidth: 33
            Layout.fillHeight: true

            ColumnLayout {
                anchors.fill: parent
                spacing: 5
                Item {Layout.preferredWidth: 1}

                ToolbarButton {
                    Layout.alignment: Qt.AlignVCenter
                    icon: "qrc:/tools/google.png"
                    onClicked: owner.searchIndex = 0
                    toggle: owner.searchIndex === 0
                }
                ToolbarButton {
                    Layout.alignment: Qt.AlignVCenter
                    icon: "qrc:/tools/yandex.png"
                    onClicked: owner.searchIndex = 1
                    toggle: owner.searchIndex === 1
                }

                Item { Layout.fillHeight: true }
            }
        }
        Item {
            Layout.fillWidth: true
            Layout.fillHeight: true
            WebEngineView {
                anchors.fill: parent
                id: google
                url: owner.googleUrl
                focus: true
                visible: owner.searchIndex === 0
                onNewViewRequested: dosearch.main.openLink(request, owner, request.destination === WebEngineView.NewViewInBackgroundTab)
                onUrlChanged: {
                    var query = owner.parseGoogleQuery(url)
                    if (query != owner.query) {
                        url = owner.googleUrl
                        dosearch.navigation.open(dosearch.search(query, 0))
                    }
                }
            }
            WebEngineView {
                anchors.fill: parent
                id: yandex
                url: owner.yandexUrl
                focus: true
                visible: owner.searchIndex === 1
                onNewViewRequested: dosearch.main.openLink(request, owner, request.destination === WebEngineView.NewViewInBackgroundTab)
                onUrlChanged: {
                    var query = owner.parseYandexQuery(url)
                    if (query != owner.query) {
                        url = owner.yandexUrl
                        dosearch.navigation.open(dosearch.search(query, 1))
                    }
                }
            }
        }
    }
}

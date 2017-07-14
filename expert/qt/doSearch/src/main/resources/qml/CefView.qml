import QtQuick 2.8
import QtQuick.Layouts 1.1
import QtQuick.Controls 2.1
import QtQuick.Controls.impl 2.1
import QtQuick.Controls.Material 2.1
import QtQuick.Controls 1.4 as Legacy
import QtQuick.Window 2.0

import ExpLeague 1.0
import "."

Rectangle{
    id: self
    property double zoom
    property alias webView: webView
    property alias url: webView.url
    property bool mute: true

    onMuteChanged: {
        webView.setMute(mute)
    }

    CefItem {
        id: webView;
        zoomFactor: zoom
        anchors.fill: parent;
        onRequestPage: {
            if(url != this.url){
                owner.open(url, newTab, false)
            }
        }

        onDragStarted: {
            dosearch.main.dragType = "web";
        }

        onCursorChanged: {
            dragArea.cursorShape = cursorShape
        }

        onDownloadStarted: {
            dosearch.navigation.select(0, dosearch.navigation.context)
            dosearch.navigation.context.ui.downloads.append(item)
        }

        onSavedToStorage: {
            dosearch.navigation.context.vault.drop(text, "", [], owner.id)
        }

        onUrlChanged: {
            focused = true
        }
        onLoadEnd: {
            focused = Qt.binding(function() {return self.visible && dosearch.main.active})
            if(self.mute)
                setMute(true)
        }

        function setMute(mute){
            if(mute){
                executeJS("var videos = document.querySelectorAll('video'), audios = document.querySelectorAll('audio');
                                [].forEach.call(videos, function(video) { video.muted = true;});
                                [].forEach.call(audios, function(audio) { audio.muted = true;});
                           ")
            }else{
                executeJS("var videos = document.querySelectorAll('video'), audios = document.querySelectorAll('audio');
                                [].forEach.call(videos, function(video) { video.muted = false});
                                [].forEach.call(audios, function(audio) { audio.muted = false});
                           ")
            }
        }

    }


    MouseArea {
        id: dragArea
        anchors.fill: parent
        hoverEnabled: true
        //propagateComposedEvents: true
        acceptedButtons: Qt.AllButtons
        onPressed: {
            webView.mousePress(mouse.x, mouse.y, mouse.button, mouse.modifiers)
            mouse.accepted = true
        }

        onReleased: {
            webView.mouseRelease(mouse.x, mouse.y, mouse.button, mouse.modifiers)
            mouse.accepted = true
        }

        onPositionChanged: {
            webView.mouseMove(mouse.x, mouse.y, mouse.buttons, mouse.modifiers)
            mouse.accepted = true
        }

        onWheel: {
            if(wheel.modifiers & Qt.ControlModifier){
                webView.zoomFactor += wheel.x > 0 ? 0.2 : -0.2
            }else{
                webView.mouseWheel(wheel.x, wheel.y, wheel.buttons, wheel.angleDelta, wheel.modifiers)
            }
            wheel.accepted = true
        }
    }

    DropArea {
        x: (dosearch.main ? dosearch.main.leftMargin : 0)
        y: 0
        width: parent.width - (dosearch.main ? (dosearch.main.rightMargin + dosearch.main.leftMargin): 0)
        height: parent.height

        onEntered: {
            if (dosearch.main.dragType != "web" && dosearch.main.dragType != "none" && dosearch.main.dragType != "")
                return
            if(drag.hasUrls && webView.dragEnterUrls(drag.x, drag.y, drag.urls, drag.action) ||
                    drag.hasText && webView.dragEnterText(drag.x, drag.y, drag.text, drag.action) ||
                    drag.hasHtml && webView.dragEnterHtml(drag.x, drag.y, drag.html, drag.action))
            {
                drag.accept()
            }
            if (dosearch.main.dragType == "" && drag.source && drag.source.toString().search("Main_QMLTYPE") >= 0) {
                dosearch.main.dragType = "web"
                dosearch.main.drag = drag.source
            }
        }
        onExited: {
            if (dosearch.main.dragType != "web" && dosearch.main.dragType != "none" && dosearch.main.dragType != "")
                return
            webView.dragExit()
            if (dosearch.main.dragType != "web")
                return
            dosearch.main.dragType == "delay"
            dosearch.main.delay(100, function () {
                if (dosearch.main.dragType == "delay") {
                    dosearch.main.dragType = ""
                    dosearch.main.drag = null
                }
            })
        }
        onPositionChanged: {
            if (dosearch.main.dragType != "web" && dosearch.main.dragType != "none" && dosearch.main.dragType != "")
                return
            if(webView.dragMove(drag.x, drag.y, drag.action))
                drag.accept()
        }

        onDropped: {
            if (dosearch.main.dragType != "web" && dosearch.main.dragType != "none" && dosearch.main.dragType != "")
                return
            //console.log("Dropped: " + drag)
            dosearch.main.drag = null
            dosearch.main.dragType = ""
            if(webView.dragDrop(drop.x, drop.y))
                drop.accept()
        }
    }

    Keys.onPressed: {
        event.accepted = webView.sendKeyPress(event)
    }

    Keys.onReleased: {
//        console.log("releaseEvent")
        event.accepted = webView.sendKeyRelease(event)
    }

    onActiveFocusChanged: {
        webView.setBrowserFocus(activeFocus)
    }
}

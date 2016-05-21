#ifndef SEARCHSCREEN_H
#define SEARCHSCREEN_H

#include <QList>
#include <QUrl>
#include <QUrlQuery>

#include <QQuickItem>
#include <QQmlListProperty>

#include "../screen.h"

namespace expleague {

class WebScreen;
class WebFolder;
bool isSearch(const QUrl& url);

class SearchRequest: public QObject {
    Q_OBJECT

    Q_PROPERTY(QString query READ query CONSTANT)
    Q_PROPERTY(int clicks READ clicks NOTIFY clicksChanged)

public:
    QString query() {
        return m_query;
    }

    int clicks() {
        return m_clicks;
    }

public:
    void setQuery(const QString& query) {
        m_query = query;
        queryChanged(query);
    }

    void registerClick(WebScreen*) {
        m_clicks++;
        clicksChanged();
    }

public:
    SearchRequest(const QString& query = "", int clicks = 0, QObject* parent = 0): QObject(parent), m_query(query), m_clicks(clicks) {}

signals:
    void queryChanged(const QString&);
    void clicksChanged();

private:
    QString m_query;
    int m_clicks;
};

class WebSearch: public Screen {
    Q_OBJECT

    Q_PROPERTY(QQmlListProperty<expleague::SearchRequest> queries READ queries NOTIFY queriesChanged)

public:
    WebSearch(QObject* parent = 0): Screen(QUrl("qrc:/WebSearchView.qml"), parent), webView(itemById<QQuickItem>("webView")) {
        connect(webView, SIGNAL(urlChanged()), SLOT(urlChanged()));
        connect(webView, SIGNAL(titleChanged()), SLOT(titleChanged()));
        connect(webView, SIGNAL(iconChanged()), SLOT(iconChanged()));
        QQuickItem* root = itemById<QQuickItem>("root");
        root->setProperty("owner", QVariant::fromValue(this));
    }

    bool handleOmniboxInput(const QString &text) {
        search(text);
        return true;
    }

    QUrl icon() const {
        return webView->property("icon").toUrl();
    }

    QString location() const {
        return webView->property("url").toString();
    }

    QString name() const {
        return webView->property("title").toString();
    }

    QQmlListProperty<SearchRequest> queries() {
        return QQmlListProperty<SearchRequest>(this, m_queries);
    }

    Q_INVOKABLE void search(const QString& text) {
        webView->setProperty("url", QUrl("https://www.google.ru/search?q=" + QUrl::toPercentEncoding(text)));
    }

    Q_INVOKABLE void wipeQuery(const QString& text) {
        for (int i = 0; i < m_queries.size(); i++) {
            SearchRequest* request = m_queries.at(i);
            if (request->query() == text) {
                m_queries.removeAt(i);
                queriesChanged();
                request->deleteLater();
            }
        }
    }

    Q_INVOKABLE QQuickItem* landing();

public:
    void append(const QString& query, int clicks) {
        m_queries.append(new SearchRequest(query, clicks, this));
    }

signals:
    void queriesChanged();

private slots:
    WebFolder* owner() {
        return (WebFolder*)parent();
    }

    void urlChanged() { // google only
        QUrl url = webView->property("url").toUrl();
        locationChanged(url.toString());
        QUrlQuery query(url.hasFragment() ? url.fragment() : url.query());
        QString queryText = query.queryItemValue("q");
        queryText.replace("+", " ");
        SearchRequest* request = 0;
        SearchRequest* last = 0;
        if (!m_queries.empty() && m_queries.last()->clicks() == 0) {
            last = m_queries.last();
            m_queries.removeLast();
        }
        for (int i = 0; i < m_queries.size(); i++) {
            if (m_queries.at(i)->query() == queryText) {
                request = m_queries.at(i);
                m_queries.removeOne(request);
            }
        }

        m_queries.append(request ? request : new SearchRequest(queryText, 0, this));
        queriesChanged();
//        last->deleteLater();
    }

    void titleChanged() {
        nameChanged(name());
    }

    void iconChanged() {
        Screen::iconChanged(icon());
    }

private:
    QQuickItem* webView;
    QList<SearchRequest*> m_queries;
};
}

QML_DECLARE_TYPE(expleague::SearchRequest)
QML_DECLARE_TYPE(expleague::WebSearch)


#endif // SEARCHSCREEN_H

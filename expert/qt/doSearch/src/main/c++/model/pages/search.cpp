#include "search.h"
#include "../../dosearch.h"

#include <QRegExp>
#include <QUrlQuery>

namespace expleague {
SearchRequest SearchRequest::EMPTY("");

QString SearchRequest::parseGoogleQuery(const QUrl& request) const {
    if (request.path() != "/search")
        return "";
    QUrlQuery query(request.hasFragment() ? request.fragment() : request.query());
    QString queryText = query.queryItemValue("q", QUrl::PrettyDecoded);
    queryText.replace("%2B", "+");
    static QRegExp site("site:(\\S+)");
    int index;
    if ((index = site.indexIn(queryText)) >= 0)
        queryText = queryText.mid(0, index) + "#site(" + site.cap(1) + ")" + queryText.mid(index + site.matchedLength());
    return queryText.trimmed();
}

QString SearchRequest::parseYandexQuery(const QUrl& request) const {
    QString path = request.path();
    if (path != "/search/" && path != "/yandsearch")
        return "";
    QUrlQuery query(request.hasFragment() ? request.fragment() : request.query());
    QString queryText = query.queryItemValue("text", QUrl::PrettyDecoded);
    queryText.replace("%2B", "+");
    static QRegExp site("host:(\\S+)");
    int index;
    if ((index = site.indexIn(queryText)) >= 0)
        queryText = queryText.mid(0, index) + "#site(" + site.cap(1) + ")" + queryText.mid(index + site.matchedLength());
    return queryText.trimmed();
}

QUrl SearchRequest::googleUrl() const {
    QString queryText = m_query;
    queryText.replace("+", "%2B");
    static QRegExp site("#site\\((\\S+)\\)");
    int index;
    if ((index = site.indexIn(queryText)) >= 0)
        queryText = queryText.mid(0, index) + "site:" + site.cap(1) + queryText.mid(index + site.matchedLength());

    QUrl result;
    result.setScheme("https");
    result.setHost("google.ru");
    result.setPath("/search");
    QUrlQuery query;
    query.addQueryItem("q", queryText);
    result.setQuery(query);
    return result;
}

QUrl SearchRequest::yandexUrl() const {
    QString queryText = m_query;
    queryText.replace("+", "%2B");
    static QRegExp site("#site\\((\\S+)\\)");
    int index;
    if ((index = site.indexIn(queryText)) >= 0)
        queryText = queryText.mid(0, index) + "host:" + site.cap(1) + queryText.mid(index + site.matchedLength());

    QUrl result;
    result.setScheme("https");
    result.setHost("yandex.ru");
    result.setPath("/search/");
    QUrlQuery query;
    query.addQueryItem("text", queryText);
    result.setQuery(query);
    return result;
}

SearchRequest::SearchRequest(const QString& id, const QString& query, doSearch* parent): Page(id, "qrc:/WebSearchView.qml", parent), m_query(query)
{
    store("search.query", query);
    SearchRequest* last = parent->navigation()->context()->lastRequest();
    m_search_index = last->searchIndex();
    store("search.engine", m_search_index);
    save();
}

SearchRequest::SearchRequest(const QString& id, doSearch* parent): Page(id, "qrc:/WebSearchView.qml", parent),
    m_query(value("search.query").toString()),
    m_search_index(value("search.engine").toInt())
{}
}

import Foundation

enum CountlyWidgetType: String {
    case nps
    case survey
    case undefined
}

enum SurveyFollowUpType: String {
    case score
    case one
    case none
}

enum WidgetQuestionType: String {
    case rating
    case text
    case undefined
}

struct CountlyWidget: Decodable {
    var _id: String?
    var app_id: String?
    var name: String?
    var msg: WidgetMessage?
    var appearance: WidgetAppearance?
    var type: String?
    var followUpType: String?
    var questions: [WidgetQuestion]?

    static func build(_ widget: [AnyHashable: Any]) -> CountlyWidget? {
        let json = try? JSONSerialization.data(withJSONObject: widget, options: [])
        let w = try? JSONDecoder().decode(CountlyWidget.self, from: json ?? Data())
        return w
    }

    var wType: CountlyWidgetType {
        guard let value = CountlyWidgetType(rawValue: self.type ?? "") else {
            return .undefined
        }
        return value
    }

    var wFollowUpType: SurveyFollowUpType {
        guard let value = SurveyFollowUpType(rawValue: self.followUpType ?? "") else {
            return .none
        }
        return value
    }
}

struct WidgetMessage: Decodable {
    var mainQuestion: String?
    var followUpAll: String?
    var followUpPromoter: String?
    var followUpPassive: String?
    var followUpDetractor: String?
    var thanks: String?
}

struct WidgetAppearance: Decodable {
    var show: String?
    var color: String?
    var style: String?
    var submit: String?
    var followUpInput: String?
    var notLikely: String?
    var likely: String?
}

struct WidgetQuestion: Decodable {
    var type: String?
    var question: String?
    var required: Bool?
    var other: Bool?
    var allOfTheAbove: Bool?
    var noneOfTheAbove: Bool?
    var otherText: String?
    var allOfTheAboveText: String?
    var noneOfTheAboveText: String?
    var notLikely: String?
    var likely: String?
    var id: String?

    var qType: WidgetQuestionType {
        guard let value = WidgetQuestionType(rawValue: self.type ?? "") else {
            return .undefined
        }
        return value
    }
}

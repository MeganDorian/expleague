//
//  AppDelegate.swift
//  unSearch
//
//  Created by Игорь Кураленок on 09.01.16.
//  Copyright (c) 2016 Experts League Inc. All rights reserved.
//


import UIKit
import XMPPFramework
import CoreData
import MMMarkdown
import FBSDKCoreKit
import Intents

import unSearchCore

class Palette {
    static let CONTROL = UIColor(red: 17/256.0, green: 138/256.0, blue: 222/256.0, alpha: 1.0)
    static let CONTROL_BACKGROUND = UIColor(red: 249/256.0, green: 249/256.0, blue: 249/256.0, alpha: 1.0)
    static let CHAT_BACKGROUND = UIColor(red: 230/256.0, green: 233/256.0, blue: 234/256.0, alpha: 1.0)
    static let OK = UIColor(red: 102/256.0, green: 182/256.0, blue: 15/256.0, alpha: 1.0)
    static let ERROR = UIColor(red: 174/256.0, green: 53/256.0, blue: 53/256.0, alpha: 1.0)
    static let COMMENT = UIColor(red: 63/256.0, green: 84/256.0, blue: 130/256.0, alpha: 1.0)
    static let BORDER = UIColor(red: 202/256.0, green: 210/256.0, blue: 227/256.0, alpha: 1.0)
    static let CORNER_RADIUS = CGFloat(8)
}

@UIApplicationMain
class AppDelegate: UIResponder {
    static let deviceId = UInt64(abs(UIDevice.current.identifierForVendor!.uuidString.hashValue))
    static let GOOGLE_API_KEY = "AIzaSyA83KOger1DEkxp3h0ItejyGBlEUuE7Bkc"
    @nonobjc static var instance: AppDelegate {
        return (UIApplication.shared.delegate as! AppDelegate)
    }
    
    static func versionName() -> String {
        let system = Bundle.main.infoDictionary!
        return "\(system["CFBundleShortVersionString"]!) build \(system["CFBundleVersion"]!)"
    }
    
    var window: UIWindow?
    
    var tabs: TabsViewController!

    var split: UISplitViewController {
        return tabs.viewControllers![1] as! UISplitViewController
    }
    
    fileprivate var navigation: UINavigationController {
        get {
            return (split.viewControllers[0] as! UINavigationController)
        }
    }

    var orderView: OrderViewController?
    var expertsView: ExpertsOverviewController?
    var historyView: HistoryViewController?
    let uploader = AttachmentsUploader()
    
    var connectionErrorNotification: UILocalNotification?
    
    func prepareBackground(_ application: UIApplication) {
        DataController.shared().xmppQueue.async {
            if(DataController.shared().activeProfile?.busy ?? false) {
                application.setMinimumBackgroundFetchInterval(UIApplicationBackgroundFetchIntervalMinimum)
            }
            else {
                application.setMinimumBackgroundFetchInterval(UIApplicationBackgroundFetchIntervalNever)
            }
        }
    }
}

extension AppDelegate: UIApplicationDelegate {
    func application(_ application: UIApplication, didFinishLaunchingWithOptions launchOptions: [UIApplicationLaunchOptionsKey: Any]?) -> Bool {
        EVURLCache.LOGGING = false
        EVURLCache.MAX_FILE_SIZE = 26
        EVURLCache.MAX_CACHE_SIZE = 30
        EVURLCache.MAX_AGE = "\(3.0 * 365 * 24 * 60 * 60 * 1000)"
        EVURLCache.FORCE_LOWERCASE = true // is already the default. You also have to put all files int he PreCache using lowercase names
        EVURLCache.activate()
        
        GMSServices.provideAPIKey(AppDelegate.GOOGLE_API_KEY)
        GMSPlacesClient.provideAPIKey(AppDelegate.GOOGLE_API_KEY)
        
        DataController.shared().version = "unSearch \(AppDelegate.versionName()) @iOS \(ProcessInfo.processInfo.operatingSystemVersionString)"
        NSSetUncaughtExceptionHandler({(e: NSException) -> () in
            print("Starck trace: \(e.callStackSymbols)")
        })
        if #available(iOS 10.0, *) {
            INPreferences.requestSiriAuthorization() { (status: INSiriAuthorizationStatus) -> Void in
                print(status)
            }
            INVocabulary.shared().setVocabularyStrings(["эксперт", "экспертов", "эксперта", "экспертам", "эксперту"], of: .contactGroupName)
        }
        
        window = UIWindow(frame: UIScreen.main.bounds)
        let storyboard = UIStoryboard(name: "Main", bundle: nil)
        DataController.shared().setupDefaultProfiles(UIDevice.current.identifierForVendor!.uuidString.hashValue)
//        window?.rootViewController = DataController.shared().activeProfile != nil ? storyboard.instantiateViewController(withIdentifier: "tabs") : storyboard.instantiateInitialViewController()
        window?.rootViewController = storyboard.instantiateViewController(withIdentifier: "tabs")
        window?.makeKeyAndVisible()
        
        application.registerForRemoteNotifications()
        let settings = UIUserNotificationSettings(types: [.alert, .sound], categories: [])
        application.registerUserNotificationSettings(settings)
        application.isIdleTimerDisabled = false
        return true
    }
    
    func applicationDidEnterBackground(_ application: UIApplication) {
        prepareBackground(application)
        DataController.shared().suspend()
    }
    
    func applicationDidBecomeActive(_ application: UIApplication) {
        FBSDKAppEvents.activateApp()
        if (connectionErrorNotification != nil) {
            application.cancelLocalNotification(connectionErrorNotification!)
        }
        DataController.shared().resume()
    }
    
    func application(_ application: UIApplication, performFetchWithCompletionHandler completionHandler: @escaping (UIBackgroundFetchResult) -> Void) {
        if (connectionErrorNotification != nil) {
            application.cancelLocalNotification(connectionErrorNotification!)
        }
        guard DataController.shared().activeProfile != nil && application.applicationState != .active else {
            completionHandler(.newData)
            return
        }
        if (!ExpLeagueProfile.active.busy) {
            completionHandler(.noData)
        }
        else if (DataController.shared().reachability?.isReachable ?? true) {
            QObject.track(ExpLeagueProfile.active, #selector(ExpLeagueProfile.busyChanged)) {
                guard !ExpLeagueProfile.active.busy else {
                    return true
                }
                self.prepareBackground(application)
                ExpLeagueProfile.active.suspend()
                DispatchQueue.main.async {
                    completionHandler(.newData)
                }
                return false
            }
            ExpLeagueProfile.active.resume()
        }
        else {
            self.prepareBackground(application)
            DispatchQueue.main.async {
                completionHandler(.failed)
            }
        }
    }
    
    func application(_ application: UIApplication, didReceiveRemoteNotification userInfo: [AnyHashable: Any], fetchCompletionHandler completionHandler: @escaping (UIBackgroundFetchResult) -> Void) {
        guard DataController.shared().activeProfile != nil else {
            completionHandler(.newData)
            return
        }
        print("Received remote notification: \(userInfo)")
        if let messageId = userInfo["id"] as? String {
            ExpLeagueProfile.active.expect(messageId)
        }
        else if let aowId = userInfo["aow"] as? String {
            ExpLeagueProfile.active.aow(aowId, title: userInfo["title"] as? String)
        }
        self.application(application, performFetchWithCompletionHandler: completionHandler)
    }

    func application(_ application: UIApplication, didReceive notification: UILocalNotification) {
        if let orderId = notification.userInfo?["order"] as? String, let order = ExpLeagueProfile.active.order(name: orderId) {
            ExpLeagueProfile.active.selectedOrder = order
            tabs.selectedIndex = 1
        }
    }
    
    func applicationWillTerminate(_ application: UIApplication) {
        ExpLeagueProfile.active.suspend()
    }
    
    func application(_ application: UIApplication, didRegisterForRemoteNotificationsWithDeviceToken deviceToken: Data) {
        let chars = (deviceToken as NSData).bytes.assumingMemoryBound(to: Swift.CChar)
        var token = ""
        
        for i in 0..<deviceToken.count {
            token += String(format: "%02.2hhx", arguments: [chars[i]])
        }
        DataController.shared().token = token
    }
}

class TabsViewController: UITabBarController {
    override func viewDidLoad() {
        AppDelegate.instance.tabs = self
        for b in tabBar.items! {
            b.image = b.image?.withRenderingMode(.alwaysOriginal)
            b.selectedImage = b.selectedImage?.withRenderingMode(.alwaysOriginal)
            b.setTitleTextAttributes([NSForegroundColorAttributeName: Palette.CONTROL], for: .selected)
        }
    }
}

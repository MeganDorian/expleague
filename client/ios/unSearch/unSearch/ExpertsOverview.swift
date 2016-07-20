//
//  ExpertsOverview.swift
//  unSearch
//
//  Created by Igor E. Kuralenok on 13/03/16.
//  Copyright © 2016 Experts League Inc. All rights reserved.
//

import Foundation
import UIKit

class ExpertsOverviewController: UITableViewController {
    var experts: [ExpLeagueMember] {
        return AppDelegate.instance.activeProfile!.experts
    }
    
    var table: UITableView {
        return (self.view as! UITableView)
    }
        
    var top: [ExpLeagueMember] = []
    var my: [ExpLeagueMember] = []
    
    func update() {
        top.removeAll()
        my.removeAll()
        for exp in experts {
            if (exp.myTasks > 0) {
                my.append(exp)
            }
            else {
                top.append(exp)
            }
        }
        my.sortInPlace() {
            return $0.myTasks > $1.myTasks
        }
        top.sortInPlace() {
            return $0.tasks > $1.tasks
        }
        table.reloadData()
    }
    
    override func viewDidLoad() {
        super.viewDidLoad()
        self.navigationController!.navigationBar.titleTextAttributes = [NSForegroundColorAttributeName : UIColor.whiteColor()]
        AppDelegate.instance.expertsView = self
        (view as! UITableView).registerClass(UITableViewCell.self, forCellReuseIdentifier: "Empty")
    }
    
    override func viewWillAppear(animated: Bool) {
        super.viewWillAppear(animated)

        table.editing = false
        if (navigationController != nil) {
            navigationController!.navigationBar.setBackgroundImage(UIImage(named: "experts_background"), forBarMetrics: .Default)
        }
        update()
    }
    
    override func viewDidAppear(animated: Bool) {
        AppDelegate.instance.tabs.tabBar.hidden = false
    }
    
    override func tableView(tableView: UITableView, canEditRowAtIndexPath indexPath: NSIndexPath) -> Bool {
        return false
    }
    
    override func tableView(tableView: UITableView, cellForRowAtIndexPath indexPath: NSIndexPath) -> UITableViewCell {
        let cell: ExpertCell
        let exp: ExpLeagueMember
        switch(indexPath.section) {
        case 0 where my.isEmpty && top.isEmpty:
            let cell = tableView.dequeueReusableCellWithIdentifier("Empty", forIndexPath: indexPath)
            cell.textLabel!.text = "Нет избранных"
            cell.textLabel!.textAlignment = .Center
            cell.textLabel!.textColor = UIColor.lightGrayColor()
            return cell
        case 0 where !my.isEmpty:
            exp = my[indexPath.row]
            cell = tableView.dequeueReusableCellWithIdentifier("FavoriteExpert", forIndexPath: indexPath) as! ExpertCell
        case 0 where my.isEmpty:
            exp = top[indexPath.row]
            cell = tableView.dequeueReusableCellWithIdentifier("TopExpert", forIndexPath: indexPath) as! ExpertCell
        case 1:
            exp = top[indexPath.row]
            cell = tableView.dequeueReusableCellWithIdentifier("TopExpert", forIndexPath: indexPath) as! ExpertCell
        default:
            return UITableViewCell()
        }
        cell.update(exp)
        return cell
    }
    
    override func tableView(tableView: UITableView, heightForRowAtIndexPath indexPath: NSIndexPath) -> CGFloat {
        return 77;
    }
    
    override func tableView(tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        switch(section) {
        case 0 where my.isEmpty && top.isEmpty:
            return 1
        case 0:
            return my.count > 0 ? my.count : top.count
        case 1:
            return top.count
        default:
            return 0
        }
    }
    
    override func tableView(tableView: UITableView, heightForHeaderInSection section: Int) -> CGFloat {
        return 38
    }
    
    override func tableView(tableView: UITableView, viewForHeaderInSection section: Int) -> UIView? {
        let label = UILabel()
        label.textColor = UIColor.lightGrayColor()
        label.font = UIFont(name: "Helvetica", size: 14)
        label.backgroundColor = UIColor.whiteColor()
        label.frame = CGRectMake(15, 0, tableView.frame.width - 15, 38)
        let view = UIView()
        view.addSubview(label)
        switch(section) {
        case 0 where my.isEmpty && top.isEmpty:
            label.text = "ИЗБРАННЫЕ"
        case 0:
            if (my.count > 0) {
                label.text = "ИЗБРАННЫЕ"
            }
            else {
                label.text = "ЛУЧШИЕ"
            }
        case 1:
            label.text = "ЛУЧШИЕ"
        default:
            label.text = ""
        }
        view.frame = CGRectMake(0, 0, tableView.frame.width, 38)
        view.backgroundColor = UIColor.whiteColor()
        return view
    }
    
    override func numberOfSectionsInTableView(tableView: UITableView) -> Int {
        return my.isEmpty || top.isEmpty ? 1 : 2
    }
    
    override func tableView(tableView: UITableView, didSelectRowAtIndexPath indexPath: NSIndexPath) {
        let exp: ExpLeagueMember
        switch(indexPath.section) {
        case 0 where my.isEmpty && top.isEmpty:
            return;
        case 0 where !my.isEmpty:
            exp = my[indexPath.row]
        case 0 where my.isEmpty:
            exp = top[indexPath.row]
        case 1:
            exp = top[indexPath.row]
        default:
            return
        }
        AppDelegate.instance.tabs.tabBar.hidden = true;
        let expertView = ExpertViewController(expert: exp)
        splitViewController!.showDetailViewController(expertView, sender: nil)
    }
    
    override func tableView(tableView: UITableView, canMoveRowAtIndexPath indexPath: NSIndexPath) -> Bool {
        return false
    }
    
    override func tableView(tableView: UITableView, shouldHighlightRowAtIndexPath indexPath: NSIndexPath) -> Bool {
        if (indexPath.section == 0 && indexPath.row == 0) {
            return !(my.isEmpty && top.isEmpty)
        }
        return true
    }
}

extension ExpertsOverviewController: UISplitViewControllerDelegate {
    func primaryViewControllerForCollapsingSplitViewController(splitViewController: UISplitViewController) -> UIViewController? {
        AppDelegate.instance.tabs.tabBar.hidden = false
        return navigationController ?? self
    }
    
    func primaryViewControllerForExpandingSplitViewController(splitViewController: UISplitViewController) -> UIViewController? {
        return primaryViewControllerForCollapsingSplitViewController(splitViewController)
    }
    
    func splitViewController(svc: UISplitViewController, willChangeToDisplayMode displayMode: UISplitViewControllerDisplayMode) {
        if (displayMode != .AllVisible) {
            AppDelegate.instance.tabs.tabBar.hidden = false
        }
    }
}

class ChooseExpertViewController: UITableViewController {
    var experts: [ExpLeagueMember] = []
    
    let parent: OrderDescriptionViewController
    
    func close() {
        parent.update()
        self.dismissViewControllerAnimated(true, completion: nil)
    }
    
    override func viewDidLoad() {
        super.viewDidLoad()
        navigationController!.navigationBarHidden = false
        navigationController!.navigationBar.setBackgroundImage(UIImage(named: "experts_background"), forBarMetrics: .Default)
        navigationController!.navigationBar.titleTextAttributes = [NSForegroundColorAttributeName : UIColor.whiteColor()]
        navigationItem.title = "Выберите эксперта"
    }
    
    override func viewWillAppear(animated: Bool) {
        experts = ExpLeagueProfile.active.experts.sort {
            return $0.myTasks != $1._myTasks ? $0.myTasks > $1.myTasks : $0.tasks > $1.tasks
        }
        (view as! UITableView).reloadData()
    }
    
    override func tableView(tableView: UITableView, canEditRowAtIndexPath indexPath: NSIndexPath) -> Bool {
        return false
    }
    
    override func tableView(tableView: UITableView, cellForRowAtIndexPath indexPath: NSIndexPath) -> UITableViewCell {
        let exp = experts[indexPath.row]
        let cell = tableView.dequeueReusableCellWithIdentifier("ExpertCell", forIndexPath: indexPath) as! ExpertCell
        cell.update(exp)
        return cell
    }
    
    override func tableView(tableView: UITableView, heightForRowAtIndexPath indexPath: NSIndexPath) -> CGFloat {
        return 77;
    }
    
    override func tableView(tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        return experts.count
    }
    
    override func tableView(tableView: UITableView, heightForHeaderInSection section: Int) -> CGFloat {
        return 0
    }
    
    override func numberOfSectionsInTableView(tableView: UITableView) -> Int {
        return 1
    }
    
    override func tableView(tableView: UITableView, didSelectRowAtIndexPath indexPath: NSIndexPath) {
        let expert = experts[indexPath.item]
        if (!parent.experts.contains(expert)) {
            parent.experts.append(expert)
        }
        else {
            tableView.deselectRowAtIndexPath(indexPath, animated: true)
            parent.experts.removeAtIndex(parent.experts.indexOf(expert)!)
        }
    }
    
    override func tableView(tableView: UITableView, willDisplayCell cell: UITableViewCell, forRowAtIndexPath indexPath: NSIndexPath) {
        cell.highlighted = parent.experts.contains(experts[indexPath.item])
    }
    
    override func tableView(tableView: UITableView, canMoveRowAtIndexPath indexPath: NSIndexPath) -> Bool {
        return false
    }
    
    override func tableView(tableView: UITableView, shouldHighlightRowAtIndexPath indexPath: NSIndexPath) -> Bool {
        return true
    }
    
    override func loadView() {
        let table = UITableView()
        view = table
        table.registerNib(UINib(nibName: "ExpertCell", bundle: NSBundle.mainBundle()), forCellReuseIdentifier: "ExpertCell")
        table.dataSource = self
        table.delegate = self
        table.separatorStyle = .None
    }
    
    init(parent: OrderDescriptionViewController) {
        self.parent = parent
        super.init(nibName: nil, bundle: nil)
    }

    required init?(coder aDecoder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
}

class ExpertCell: UITableViewCell {
    @IBOutlet weak var name: UILabel!
    @IBOutlet weak var tags: UILabel!
    @IBOutlet weak var tasks: UILabel!
    @IBOutlet weak var avatar: AvatarView!
    
    func update(expert: ExpLeagueMember) {
        name.text = expert.name
        tags.text = expert.tags.joinWithSeparator(", ")
        avatar.image = expert.avatar
        avatar.online = expert.available
        switch (expert.group) {
        case .Favorites:
            tasks.text = "заданий: \(expert.tasks), ваших: \(expert.myTasks)"
            break
        case .Top:
            tasks.text = "заданий: \(expert.tasks)"
            break
        }

        expert.badge = self
        layoutIfNeeded()
    }
}



/* Modified slightly for use in Green from the public release at
 * https://github.com/maail/MMSlidingButton
 */

import Foundation
import UIKit

@objc protocol SlideButtonDelegate{
    @available(*, unavailable, message: "Update to \"unlocked()\" delegate method")
    func buttonStatus(status:String, sender:SlidingButton)

    func completed(slidingButton: SlidingButton)

    @objc optional func didEnterUnlockRegion(slidingButton: SlidingButton)
    @objc optional func didExitUnlockRegion(slidingButton: SlidingButton)

}

@IBDesignable class SlidingButton: UIView{

    @objc weak var delegate: SlideButtonDelegate?

    @objc @IBInspectable var dragPointWidth: CGFloat = 42 {
        didSet{
            setStyle()
        }
    }

    @objc @IBInspectable var dragPointColor: UIColor = UIColor.darkGray {
        didSet{
            setStyle()
        }
    }

    @objc @IBInspectable var buttonColor: UIColor = UIColor.gray {
        didSet{
            setStyle()
        }
    }

    @objc @IBInspectable var buttonText: String = "UNLOCK" {
        didSet{
            setStyle()
        }
    }

    @objc @IBInspectable var buttonAttributedText: NSAttributedString? = nil {
        didSet{
            if (buttonAttributedText != nil)
            {
                let buttonText_new=buttonAttributedText!.string
                ////

                if (buttonText_new.count==0) {
                    buttonAttributedText=nil

                } else if (buttonText != buttonText_new) {
                    buttonText=buttonText_new

                } else {
                    setStyle()
                }
            }
            else
            {
                buttonText=""
            }
        }
    }

    @objc @IBInspectable var offsetButtonTextByDragPointWidth: Bool = false {
        didSet{
            setStyle()
        }
    }

    @objc @IBInspectable var imageName: UIImage = UIImage() {
        didSet{
            setStyle()
        }
    }

    @objc @IBInspectable var buttonTextColor: UIColor = UIColor.white {
        didSet{
            setStyle()
        }
    }

    @objc @IBInspectable var dragPointTextAlignment: NSTextAlignment = NSTextAlignment.center {
        didSet{
            setStyle()
        }
    }

    @objc @IBInspectable var dragPointTextColor: UIColor = UIColor.white {
        didSet{
            setStyle()
        }
    }

    @objc @IBInspectable var buttonUnlockedTextColor: UIColor = UIColor.white {
        didSet{
            setStyle()
        }
    }

    @objc @IBInspectable var buttonCornerRadius: CGFloat = 0 {
        didSet{
            setStyle()
        }
    }

    @objc @IBInspectable var buttonUnlockedText: String   = "UNLOCKED"
    @objc @IBInspectable var buttonUnlockedColor: UIColor = UIColor.black
    @objc var buttonFont                                  = UIFont.systemFont(ofSize: 17)

    @objc @IBInspectable var optionalButtonUnlockingText: String   = ""

    @objc @IBInspectable var invertSwipeDirection: Bool = false


    @objc private(set) var dragPoint            = UIView()
    @objc private(set) var buttonLabel          = UILabel()
    @objc private(set) var dragPointButtonLabel = UILabel()
    @objc private(set) var imageView            = UIImageView()
    @objc private(set) var unlocked             = false
    @objc private var layoutSet            = false

    private var isInsideUnlockRegion = false
    private var dispatchCounterFor_isInsideUnlockRegionDidChange : UInt16 = 0

    private var panGestureRecognizer : UIPanGestureRecognizer?

    override init (frame : CGRect) {
        super.init(frame : frame)
    }

    required init(coder aDecoder: NSCoder) {
        super.init(coder: aDecoder)!
    }

    override func layoutSubviews() {
        if !layoutSet{
            self.setUpButton()
            self.layoutSet = true
        }
        else {
            let dragPointWidthDifference=(self.frame.size.width-self.dragPoint.frame.size.width);

            if (panGestureRecognizer?.state == UIGestureRecognizer.State.began || panGestureRecognizer?.state == UIGestureRecognizer.State.changed) {
                panGestureRecognizer?.isEnabled=false // NOTE: This is to cancel any current gesture during a rotation/resize.
                panGestureRecognizer?.isEnabled=true
                ////

                if (self.invertSwipeDirection) {
                    self.dragPoint.frame.origin.x=dragPointDefaultOriginX() - dragPointWidthDifference;
                } else {
                    self.dragPoint.frame.origin.x=dragPointDefaultOriginX() + dragPointWidthDifference;
                }

            }
            ////

            func fixWidths() {
                for (_, view) in [self.dragPoint, self.dragPointButtonLabel].enumerated() {
                    view.frame.size.width=view.frame.size.width + dragPointWidthDifference;
                }

            }

            func fixOffsets() {
                if (self.invertSwipeDirection) {
                    self.dragPoint.frame.origin.x=self.dragPoint.frame.origin.x + dragPointWidthDifference;
                } else {
                    self.dragPoint.frame.origin.x=self.dragPoint.frame.origin.x - dragPointWidthDifference;
                    self.imageView.frame.origin.x=self.imageView.frame.origin.x + dragPointWidthDifference;
                }

            }
            ////

            let widthsBeforeOffsets=(dragPointWidthDifference>=0);

            if (widthsBeforeOffsets) {
                fixWidths();
            } else {
                fixOffsets();
            }

            DispatchQueue.main.async {
                if (widthsBeforeOffsets) {
                    fixOffsets();
                } else {
                    fixWidths();
                }
            }
        }
    }

    private func dragPointButtonText() -> String {
        if (optionalButtonUnlockingText.count > 0) {
            return optionalButtonUnlockingText
        } else {
            return buttonText
        }
    }

    private func configureButtonLabel() {
        self.buttonLabel.textColor          = self.buttonTextColor

        if (self.buttonAttributedText != nil) {
            self.buttonLabel.attributedText     = self.buttonAttributedText
        } else {
            self.buttonLabel.text               = self.buttonText
        }
    }

    func setStyle(){
        configureButtonLabel()
        ////

        self.dragPointButtonLabel.text      = dragPointButtonText()
        self.dragPoint.frame.size.width     = self.dragPointWidth
        self.dragPoint.backgroundColor      = self.dragPointColor
        self.backgroundColor                = self.buttonColor
        self.imageView.image                = imageName
        self.dragPointButtonLabel.textColor = self.dragPointTextColor

        self.dragPoint.layer.cornerRadius   = buttonCornerRadius
        self.layer.cornerRadius             = buttonCornerRadius
    }

    fileprivate func dragPointDefaultOriginX() -> CGFloat {
        if (invertSwipeDirection)
        {
            return self.frame.size.width - dragPointWidth;
        }

        return dragPointWidth - self.frame.size.width;

    }

    fileprivate func dragPointButtonLabelTextAlignment() -> NSTextAlignment {
        var textAlignment = dragPointTextAlignment

        if (unlocked) {
            textAlignment = .center
        }

        return textAlignment

    }

    fileprivate func setUpButton(){

        self.backgroundColor              = self.buttonColor

        self.dragPoint                    = UIView(frame: CGRect(x: dragPointDefaultOriginX(), y: 0, width: self.frame.size.width, height: self.frame.size.height))
        self.dragPoint.autoresizingMask=[UIView.AutoresizingMask.flexibleHeight]

        self.dragPoint.backgroundColor    = dragPointColor
        let gradientLayer = self.dragPoint.makeGradient(colours: [UIColor.customMatrixGreen(), UIColor.customMatrixGreenDark()], locations: nil)
        self.dragPoint.layer.insertSublayer(gradientLayer, at: 0)
        self.dragPoint.layer.cornerRadius = buttonCornerRadius
        self.addSubview(self.dragPoint)

        if !self.buttonText.isEmpty{
            var dragPointX : CGFloat = 0;

            if (offsetButtonTextByDragPointWidth && !invertSwipeDirection)
            {
                dragPointX=dragPointWidth;
            }
            ////

            self.buttonLabel               = UILabel(frame: CGRect(x: dragPointX, y: 0, width: self.frame.size.width - dragPointWidth, height: self.frame.size.height))
            self.buttonLabel.autoresizingMask=[UIView.AutoresizingMask.flexibleWidth, UIView.AutoresizingMask.flexibleHeight]

            self.buttonLabel.adjustsFontSizeToFitWidth=true
            self.buttonLabel.minimumScaleFactor=0.6
            self.buttonLabel.textAlignment = .center
            self.buttonLabel.font          = self.buttonFont
            self.buttonLabel.frame = CGRect(x: 0, y: 0, width: self.frame.width, height: self.frame.height)
            configureButtonLabel()
            self.addSubview(self.buttonLabel)

            self.dragPointButtonLabel               = UILabel(frame: CGRect(x: dragPointWidth, y: 0, width: self.frame.size.width - (dragPointWidth * 2), height: self.frame.size.height))

            self.dragPointButtonLabel.adjustsFontSizeToFitWidth=true
            self.dragPointButtonLabel.minimumScaleFactor=0.6
            self.dragPointButtonLabel.textAlignment = dragPointButtonLabelTextAlignment()
            self.dragPointButtonLabel.text          = dragPointButtonText()
            self.dragPointButtonLabel.textColor     = UIColor.white
            self.dragPointButtonLabel.font          = self.buttonFont
            self.dragPointButtonLabel.textColor     = self.dragPointTextColor
            self.dragPoint.addSubview(self.dragPointButtonLabel)
        }
        self.bringSubviewToFront(self.dragPoint)

        if self.imageName != UIImage(){
            let width: CGFloat = 17
            let height: CGFloat = 13

            var dragPointImageX : CGFloat = (self.frame.size.width - (dragPointWidth + width) / 2);

            if (invertSwipeDirection) {
                dragPointImageX=0;
            }

            let ypoint = (self.frame.height - height) / 2
            self.imageView = UIImageView(frame: CGRect(x: dragPointImageX, y: ypoint, width: 17, height: height))

            self.imageView.contentMode = .scaleAspectFill
            self.imageView.image = self.imageName
            self.dragPoint.addSubview(self.imageView)
        }

        self.layer.masksToBounds = true

        // start detecting pan gesture
        panGestureRecognizer=UIPanGestureRecognizer(target: self, action: #selector(self.panDetected(sender:)))
        panGestureRecognizer!.minimumNumberOfTouches = 1
        self.dragPoint.addGestureRecognizer(panGestureRecognizer!)
    }

    @objc func panDetected(sender: UIPanGestureRecognizer){
        var translatedPoint = sender.translation(in: self)
        translatedPoint     = CGPoint(x: translatedPoint.x, y: self.frame.size.height / 2)

        if (invertSwipeDirection) {
            sender.view?.frame.origin.x = max(0, min(dragPointDefaultOriginX(), dragPointDefaultOriginX()+translatedPoint.x));

        } else {
            sender.view?.frame.origin.x = min(0, max(dragPointDefaultOriginX(), dragPointDefaultOriginX()+translatedPoint.x));
        }
        ////

        let wasInsideUnlockRegion=isInsideUnlockRegion
        let velocityX=(sender.velocity(in: self).x * 0.2)

        func didCrossThresholdPointX(includingVelocity: Bool) -> Bool {
            var velocityXToUse=velocityX;

            if (!includingVelocity) {
                velocityXToUse=0
            }
            ////

            var thresholdXAmount : CGFloat = 60;

            if (invertSwipeDirection) {
                return (self.frame.size.width+(translatedPoint.x + velocityXToUse)) < self.dragPointWidth + thresholdXAmount;
            } else {
                thresholdXAmount = (self.frame.size.width - thresholdXAmount)
            }

            return (((translatedPoint.x + velocityXToUse) + self.dragPointWidth) > thresholdXAmount);

        }

        if (sender.state == .ended) {
            isInsideUnlockRegion=didCrossThresholdPointX(includingVelocity: true)

            if (isInsideUnlockRegion) {
                unlocked=true
                self.unlock()
            }
            ////

            UIView.transition(with: self, duration: abs(Double(velocityX) * 0.0002) + 0.2, options: UIView.AnimationOptions.curveEaseOut, animations: {
            }, completion: { (Status) in
                if Status {
                    self.animationFinished()
                }

            } )

        } else {
            isInsideUnlockRegion=didCrossThresholdPointX(includingVelocity: false)

        }
        ////

        if (wasInsideUnlockRegion != isInsideUnlockRegion) {
            isInsideUnlockRegionDidChange(newValue: isInsideUnlockRegion)
        }

    }

    fileprivate func animationFinished(){
        if !unlocked{
            self.reset()
        }
    }

    fileprivate func isInsideUnlockRegionDidChange(newValue : Bool) {
        dispatchCounterFor_isInsideUnlockRegionDidChange += 1
        let localCopyOf_dispatchCounterFor_isInsideUnlockRegionDidChange = dispatchCounterFor_isInsideUnlockRegionDidChange

        DispatchQueue.main.asyncAfter(deadline: .now() + 0.15) { // NOTE: This is meant to "debounce" the slide threshold.
            if (localCopyOf_dispatchCounterFor_isInsideUnlockRegionDidChange != self.dispatchCounterFor_isInsideUnlockRegionDidChange) {
                return

            }
            ////

            if (newValue) {
                self.delegate?.didEnterUnlockRegion?(slidingButton: self)
            } else {
                self.delegate?.didExitUnlockRegion?(slidingButton: self)
            }
        }
    }

    //lock button animation (SUCCESS)
    func unlock(){
        UIView.transition(with: self, duration: 0.2, options: .curveEaseOut, animations: {
            self.dragPoint.frame.origin.x=self.frame.size.width - self.dragPoint.frame.size.width;

        }) { (Status) in
            if Status{
                self.dragPointButtonLabel.text      = self.buttonUnlockedText
                self.imageView.isHidden               = true
                self.dragPoint.backgroundColor      = self.buttonUnlockedColor
                self.dragPointButtonLabel.textAlignment = self.dragPointButtonLabelTextAlignment()
                self.dragPointButtonLabel.textColor = self.buttonUnlockedTextColor
                self.delegate?.completed(slidingButton: self)
            }
        }
    }

    //reset button animation (RESET)
    @objc func reset(){
        if (isInsideUnlockRegion) {
            isInsideUnlockRegion=false
            ////

            isInsideUnlockRegionDidChange(newValue: false);
        }

        UIView.transition(with: self, duration: 0.2, options: .curveEaseOut, animations: {
            self.dragPoint.frame.origin.x=self.dragPointDefaultOriginX();

        }) { (Status) in
            if Status{
                self.dragPointButtonLabel.text      = self.dragPointButtonText()
                self.imageView.isHidden               = false
                self.dragPoint.backgroundColor      = self.dragPointColor
                self.dragPointButtonLabel.textColor = self.dragPointTextColor
                self.unlocked                       = false
                ////

                self.dragPointButtonLabel.textAlignment = self.dragPointButtonLabelTextAlignment() // NOTE: Ensures this is reset after unlock.
            }
        }
    }
}

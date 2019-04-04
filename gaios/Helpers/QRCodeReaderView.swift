import AVFoundation
import UIKit

protocol QRCodeReaderDelegate: class {
    func onQRCodeReadSuccess(result: String)
}

class QRCodeReaderView: UIView {

    private let sessionQueue = DispatchQueue(label: "capture session queue", qos: .userInteractive)

    var captureSession = AVCaptureSession()
    var captureMetadataOutput: AVCaptureMetadataOutput?

    lazy var previewLayer: AVCaptureVideoPreviewLayer? = {
        let previewLayer = AVCaptureVideoPreviewLayer(session: captureSession)
        previewLayer.videoGravity = .resizeAspectFill
        return previewLayer
    }()

    lazy var blurEffectView: UIVisualEffectView = {
        let blurEffect = UIBlurEffect(style: .light)
        let blurEffectView = UIVisualEffectView(effect: blurEffect)
        blurEffectView.frame = frame
        blurEffectView.layer.masksToBounds = true
        blurEffectView.alpha = 0.9
        return blurEffectView
    }()

    lazy var borderView: UIView = {
        let borderView = UIView(frame: frame)
        borderView.backgroundColor = UIColor(white: 0, alpha: 0)
        borderView.borderWidth = 1
        borderView.borderColor = UIColor.white
        return borderView
    }()

    lazy var placeholderTextView: UIView = {
        let placeholderTextView = UIView(frame: frame)
        placeholderTextView.backgroundColor = UIColor.customTitaniumDark()
        let label = UIButton(frame: CGRect(x: 0, y: 0, width: 200, height: 40))
        placeholderTextView.addSubview(label)

        label.translatesAutoresizingMaskIntoConstraints = true
        label.center = CGPoint(x: placeholderTextView.bounds.midX, y: placeholderTextView.bounds.midY)
        label.autoresizingMask = [.flexibleLeftMargin, .flexibleRightMargin, .flexibleTopMargin, .flexibleBottomMargin]
        label.setTitle(NSLocalizedString("id_please_enable_camera", comment: ""), for: .normal)
        label.setTitleColor(UIColor.customTitaniumMedium(), for: .normal)
        label.titleLabel?.adjustsFontSizeToFitWidth = true
        label.backgroundColor = UIColor.customTitaniumDark()
        label.addTarget(self, action: #selector(onAllowCameraTap), for: .touchUpInside)

        return placeholderTextView
    }()

    var authorizationStatus: AVAuthorizationStatus!

    weak var delegate: QRCodeReaderDelegate?

    override init(frame: CGRect) {
        super.init(frame: frame)
        setupView()
    }

    required init?(coder aDecoder: NSCoder) {
        super.init(coder: aDecoder)
        setupView()
    }

    private func setupSession() {
        captureSession.beginConfiguration()
        defer {
            captureSession.commitConfiguration()
        }

        guard let captureDevice = AVCaptureDevice.default(for: .video) else {
            return
        }

        guard let captureDeviceInput = try? AVCaptureDeviceInput(device: captureDevice) else {
            return
        }

        guard captureSession.canAddInput(captureDeviceInput) else {
            return
        }

        captureSession.addInput(captureDeviceInput)

        captureMetadataOutput = AVCaptureMetadataOutput()
        guard captureSession.canAddOutput(captureMetadataOutput!) else {
            return
        }

        captureSession.addOutput(captureMetadataOutput!)
        captureMetadataOutput!.setMetadataObjectsDelegate(self, queue: sessionQueue)
        captureMetadataOutput!.metadataObjectTypes = self.captureMetadataOutput!.availableMetadataObjectTypes
    }

    private func setupCaptureView() {
        if previewLayer != nil {
            layer.addSublayer(previewLayer!)
        }
        addSubview(blurEffectView)
        addSubview(borderView)
    }

    private func setupPlaceholderView() {
        addSubview(placeholderTextView)
    }

    private func setupView() {
        backgroundColor = UIColor.customTitaniumDark()
        requestVideoAccess(presentingViewController: nil)
        sessionQueue.async {
            if self.authorizationStatus == .authorized {
                DispatchQueue.main.async {
                    self.setupSession()
                    self.setupCaptureView()
                }
            } else {
                DispatchQueue.main.async {
                    self.setupPlaceholderView()
                }
            }
        }
    }

    override func layoutSubviews() {
        blurEffectView.frame = frame
        previewLayer?.frame = frame
        placeholderTextView.frame = frame

        borderView.frame = createFrame(frame: frame)

        let maskLayer = createMaskLayer()
        if #available(iOS 11.0, *) {
            blurEffectView.layer.mask = maskLayer
        } else {
            let maskView = UIView(frame: frame)
            maskView.backgroundColor = UIColor.clear
            maskView.layer.mask = maskLayer
            blurEffectView.mask = maskView
        }
    }

    func createBorderView(frame: CGRect) -> UIView {
        let rect = createFrame(frame: frame)
        let borderView = UIView(frame: rect)
        borderView.backgroundColor = UIColor(white: 0, alpha: 0)
        borderView.borderWidth = 4
        borderView.borderColor = UIColor.red
        return borderView
    }

    private func createMaskLayer() -> CALayer {
        let rect = createFrame(frame: frame)
        let path = UIBezierPath(rect: frame)
        let centerRectangle = UIBezierPath(rect: rect)
        path.append(centerRectangle)
        path.usesEvenOddFillRule = true

        let maskLayer = CAShapeLayer()
        maskLayer.path = path.cgPath
        maskLayer.fillRule = kCAFillRuleEvenOdd
        return maskLayer
    }

    private func createFrame(frame: CGRect) -> CGRect {
        var rect = CGRect(x: 0.0, y: 0.0, width: frame.size.width / 2, height: frame.size.width / 2)
        rect.origin.x = frame.size.width / 2 - rect.size.width / 2
        rect.origin.y = frame.size.height / 2 - rect.size.height / 2
        return rect
    }

    func startScan() {
#if !(arch(i386) || arch(x86_64))
        DispatchQueue.main.async {
            if !self.captureSession.isRunning && self.authorizationStatus == .authorized {
                self.captureSession.startRunning()
                if let rectOfInterest = self.previewLayer?.metadataOutputRectConverted(fromLayerRect: self.borderView.frame) {
                    self.captureMetadataOutput?.rectOfInterest = rectOfInterest
                }
            }
        }
#endif
    }

    func isSessionNotDetermined() -> Bool {
        return authorizationStatus == .notDetermined
    }

    func isSessionAuthorized() -> Bool {
        return authorizationStatus == .authorized
    }

    func stopScan() {
#if !(arch(i386) || arch(x86_64))
        if captureSession.isRunning {
            captureSession.stopRunning()
        }
#endif
    }

    @objc func onAllowCameraTap(_ sender: Any) {
        _ = requestVideoAccess(presentingViewController: self.findViewController())
    }

    func requestVideoAccess(presentingViewController: UIViewController?) {
        authorizationStatus = AVCaptureDevice.authorizationStatus(for: .video)

        if authorizationStatus == .notDetermined {
            sessionQueue.suspend()
            AVCaptureDevice.requestAccess(for: .video, completionHandler: { (granted: Bool) in
                if granted {
                    self.authorizationStatus = .authorized
                }
                // not authorized
                self.sessionQueue.resume()
            })
        } else if authorizationStatus == .denied {
            let alert = UIAlertController(title: NSLocalizedString("id_please_enable_camera", comment: ""), message: NSLocalizedString("id_we_use_the_camera_to_scan_qr", comment: ""), preferredStyle: .alert)
            alert.addAction(UIAlertAction(title: NSLocalizedString("id_cancel", comment: ""), style: .cancel) { _ in })
            alert.addAction(UIAlertAction(title: NSLocalizedString("id_next", comment: ""), style: .default) { _ in
                if let url = URL(string: UIApplicationOpenSettingsURLString) {
                    if UIApplication.shared.canOpenURL(url) {
                        UIApplication.shared.open(url, options: [:], completionHandler: nil)
                    }
                }
            })
            DispatchQueue.main.async {
                presentingViewController?.present(alert, animated: true, completion: nil)
            }
        }
    }
}

extension QRCodeReaderView: AVCaptureMetadataOutputObjectsDelegate {
    func metadataOutput(_ output: AVCaptureMetadataOutput, didOutput metadataObjects: [AVMetadataObject], from connection: AVCaptureConnection) {

        if let metadataObject = metadataObjects.first {
            guard let readableObject = metadataObject as? AVMetadataMachineReadableCodeObject else {
                return
            }
            guard let stringValue = readableObject.stringValue else {
                return
            }
            DispatchQueue.main.async {
                self.delegate?.onQRCodeReadSuccess(result: stringValue)
            }
        }
    }
}

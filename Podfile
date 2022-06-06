# Uncomment the next line to define a global platform for your project
platform :ios, '11.0'
install! 'cocoapods', :warn_for_unused_master_specs_repo => false

target 'gaios' do
  # Comment the next line if you're not using Swift and don't want to use dynamic frameworks
  use_frameworks!

  # Pods for Green
  pod 'PromiseKit', '6.10.0'
  pod 'SwiftLint', '0.35.0'
  pod 'RxSwift', '~> 5.1'
  pod 'RxBluetoothKit', :git => 'https://github.com/chamira-at/RxBluetoothKit.git', :branch => 'ios-15-support'
  pod 'SwiftCBOR', '0.4.5'
#  pod 'Countly', '20.11.1'
  pod 'Countly', :git => 'https://github.com/angelix/countly-sdk-ios', :commit => 'ef6533f6603647dfb5f37e82952fd1b3ef36986b'

end

target 'gaiosTests' do
  inherit! :search_paths
  # Pods for testing
end

target 'gaiosUITests' do
  inherit! :search_paths
  # Pods for testing
end

post_install do |installer|
  installer.pods_project.targets.each do |target|
    target.build_configurations.each do |config|
      config.build_settings['IPHONEOS_DEPLOYMENT_TARGET'] = '11.0'
      config.build_settings['CODE_SIGNING_ALLOWED'] = 'NO'
      config.build_settings['CODE_SIGNING_REQUIRED'] = 'NO'
    end
  end
end

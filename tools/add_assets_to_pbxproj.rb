require 'xcodeproj'

project_path = 'gaios.xcodeproj'
project = Xcodeproj::Project.open(project_path)
main_group = project.main_group["gaios"]
group = main_group.new_group("AssetsRegistry", path="AssetsRegistry")

main_target = project.targets.first

Dir.foreach('gaios/AssetsRegistry') do |filename|
  next if filename == '.' or filename == '..'
  # Do work on the remaining files & directories
  file = group.new_file(filename)
  main_target.add_file_references([file])
end

project.save

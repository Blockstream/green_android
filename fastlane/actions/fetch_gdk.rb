module Fastlane
  module Actions

    class FetchGdkAction < Action
      def self.run(params)
        sh "./tools/fetch_gdk_binaries.sh"
      end

      #####################################################
      # @!group Documentation
      #####################################################

      def self.description
        "A short description with <= 80 characters of what this action does"
      end

      def self.details
        # Optional:
        # this is your chance to provide a more detailed description of this action
        "You can use this action to do cool things..."
      end

      def self.available_options
        # Define all options your action supports.

        # Below a few examples
        [
          FastlaneCore::ConfigItem.new(key: :api_token,
                                       env_name: "FL_FETCH_GDK_API_TOKEN", # The name of the environment variable
                                       description: "API Token for FetchGdkAction", # a short description of this parameter
                                       verify_block: proc do |value|
                                          UI.user_error!("No API token for FetchGdkAction given, pass using `api_token: 'token'`") unless (value and not value.empty?)
                                          # UI.user_error!("Couldn't find file at path '#{value}'") unless File.exist?(value)
                                       end),
          FastlaneCore::ConfigItem.new(key: :development,
                                       env_name: "FL_FETCH_GDK_DEVELOPMENT",
                                       description: "Create a development certificate instead of a distribution one",
                                       is_string: false, # true: verifies the input is a string, false: every kind of value
                                       default_value: false) # the default value if the user didn't provide one
        ]
      end

      def self.output
        # Define the shared values you are going to provide
        # Example
        [
          ['FETCH_GDK_CUSTOM_VALUE', 'A description of what this value contains']
        ]
      end

      def self.is_supported?(platform)
          [:ios, :mac].include?(platform)
      end
    end
  end
end

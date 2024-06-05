#!/bin/bash

# Check if the input XML file is provided
#if [ -z "$1" ]; then
#  echo "Usage: $0 <input_xml_file>"
#  exit 1
#fi

#INPUT_XML="$1"
#OUTPUT_KT="StringResources.kt"

INPUT_XML="common/src/commonMain/composeResources/values/strings.xml"
OUTPUT_KT="common/src/commonMain/kotlin/com/blockstream/common/utils/StringResources.kt"

# Extract name and value fields from the XML
entries=$(xmllint --xpath '//string' "$INPUT_XML" | sed -n 's/.*name="\([^"]*\)".*>\(.*\)<\/string>/\1 \2/p')

# Start writing the Kotlin file
echo "package com.blockstream.common.utils" > "$OUTPUT_KT"
echo "" >> "$OUTPUT_KT"
echo "import org.jetbrains.compose.resources.StringResource" >> "$OUTPUT_KT"
echo "import blockstream_green.common.generated.resources.*" >> "$OUTPUT_KT"
echo "" >> "$OUTPUT_KT"
echo "object StringResourcesMap {" >> "$OUTPUT_KT"
echo "    val strings: Map<String, StringResource> = mapOf(" >> "$OUTPUT_KT"

# Add extracted entries to the Kotlin file
while IFS= read -r entry; do
  name=$(echo "$entry" | awk '{print $1}')
  value=$(echo "$entry" | cut -d' ' -f2-)
  echo "        \"$name\" to Res.string.$name," >> "$OUTPUT_KT"
done <<< "$entries"

# Close the Kotlin map and object
echo "    )" >> "$OUTPUT_KT"
echo "}" >> "$OUTPUT_KT"

echo "Kotlin file generated: $OUTPUT_KT"

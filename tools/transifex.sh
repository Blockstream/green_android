#!/bin/bash
set -e

# --- Help
help_message() {
  cat <<- _EOF_
  Update translation strings from transifex platform

  Usage: $SCRIPT_NAME [-h|--help] -t|--token

  Options:
    -t, --transifex Transifex token
    -h, --help  Display this help message and exit

_EOF_
  exit 0
}

# --- Argument handling
# https://stackoverflow.com/questions/192249/how-do-i-parse-command-line-arguments-in-bash
POSITIONAL=()
while [[ $# -gt 0 ]]
do
key="$1"

case $key in
    -h | --help)
      help_message ;;
    -t | --token)
      TOKEN=${2}
      shift 2;;
    *)    # unknown option
    POSITIONAL+=("$1") # save it in an array for later
    shift # past argument
    ;;
esac
done

set -- "${POSITIONAL[@]:-}" # restore positional parameters

if [ -z ${TOKEN} ]; then
    echo "Need to pass a valid transifex token"
    exit 0
fi

# --- Pre-requisites
function check_command() {
    command -v $1 >/dev/null 2>&1 || { echo >&2 "$1 not found, exiting."; exit 1; }
}
check_command python3
check_command iconv

# --- Build virtualenv
export LC_CTYPE="en_US.UTF-8"
python3 -m virtualenv venv
source venv/bin/activate
pip install transifex-client lxml

# --- Fetch transifex
tx pull -f -a -s

function copy_translations {
  lang=$1
  lang_ext=$2
  file_source=translations/blockstream-green.localizablestrings/${lang}.strings
  dest_strings=gaios/${lang_ext}.lproj/Localizable.strings
  dest_permissions=gaios/${lang_ext}.lproj/InfoPlist.strings
  iconv -f UTF-16 -t UTF-8 $file_source > tmp.strings
  python tools/create_permission_strings_file.py -i tmp.strings -p tools/permission.txt -o $dest_permissions
  mv tmp.strings $dest_strings
}

echo "Copying default strings"
cp Localizable.strings translations/blockstream-green.localizablestrings/en.strings
copy_translations 'en' 'en'

echo "Copying translations"
copy_translations 'de' 'de'
copy_translations 'fr' 'fr'
copy_translations 'es' 'es'
copy_translations 'he' 'he'
copy_translations 'it' 'it'
copy_translations 'ja' 'ja'
copy_translations 'ko' 'ko'
copy_translations 'nl' 'nl'
copy_translations 'pt_BR' 'pt-BR'
copy_translations 'ru' 'ru'
copy_translations 'uk' 'uk'
copy_translations 'vi' 'vi'
copy_translations 'zh' 'zh'
copy_translations 'cs' 'cs'
copy_translations 'ro' 'ro'

rm -rf translations
deactivate

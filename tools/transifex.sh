#!/bin/bash
set -e

export LC_CTYPE="en_US.UTF-8"
python3 -m virtualenv venv
source venv/bin/activate
pip install transifex-client lxml

# fetch transifex
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

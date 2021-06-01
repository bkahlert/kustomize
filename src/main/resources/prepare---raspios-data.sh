#!/bin/sh

_p
avahi_services_dir="$BASEDIR/custom---raspios-data/avahi-services"
if [ -r "${avahi_services_dir}" ]; then
  if [ -r etc ] && [ -w etc/init.d ]; then
    _prompt "Configure Avahi?" "Y n" "custom---raspios-data/avahi-services"
    case $REPLY in
    n)
      _p "Skipping."
      ;;
    *)
      _p "Configuring Avahi … "
      for f in ${avahi_services_dir}/*; do
        cp "$f" "etc/avahi/services"
      done
      chmod +x etc/avahi/services/*
      _p "Configured Avahi successfully"
      ;;
    esac
  else
    _warn "Cannot write to etc/avahi/services. Skipping Avahi configuration."
  fi
else
  _p "No $avahi_services_dir found. Skipping Avahi configuration."
fi

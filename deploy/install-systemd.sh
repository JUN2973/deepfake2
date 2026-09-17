#!/usr/bin/env bash
set -Eeuo pipefail

if [[ "${EUID}" -ne 0 ]]; then
  echo "Run this installer with sudo." >&2
  exit 1
fi

app_user="${1:-ec2-user}"
if [[ ! "${app_user}" =~ ^[a-z_][a-z0-9_-]*$ ]]; then
  echo "Invalid Linux user name: ${app_user}" >&2
  exit 1
fi
if ! id "${app_user}" >/dev/null 2>&1; then
  echo "Linux user does not exist: ${app_user}" >&2
  exit 1
fi

script_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
service_source="${script_dir}/deepscan.service"
deploy_source="${script_dir}/ec2-deploy.sh"
if [[ ! -f "${service_source}" ]]; then
  echo "Missing service template: ${service_source}" >&2
  exit 1
fi
if [[ ! -f "${deploy_source}" ]]; then
  echo "Missing deployment helper: ${deploy_source}" >&2
  exit 1
fi

install -d -m 0750 -o "${app_user}" -g "${app_user}" /spring_module
install -d -m 0750 -o "${app_user}" -g "${app_user}" /spring_module/releases
install -d -m 0750 -o "${app_user}" -g "${app_user}" /spring_module/uploads

sed \
  -e "s/^User=.*/User=${app_user}/" \
  -e "s/^Group=.*/Group=${app_user}/" \
  "${service_source}" >/etc/systemd/system/deepscan.service
chmod 0644 /etc/systemd/system/deepscan.service
install -o root -g root -m 0755 "${deploy_source}" /usr/local/sbin/deepscan-deploy

systemctl daemon-reload
systemctl enable deepscan.service

if [[ ! -f /spring_module/.env.properties ]]; then
  echo "WARNING: create /spring_module/.env.properties before the first deployment." >&2
fi

echo "DeepScan service and deployment helper installed."
echo "Allow the CI user to run only /usr/local/sbin/deepscan-deploy through sudo."
echo "The first deployment will create current.war."

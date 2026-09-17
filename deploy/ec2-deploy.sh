#!/usr/bin/env bash
set -Eeuo pipefail

if [[ "${EUID}" -ne 0 ]]; then
  echo "Run this deployment script with sudo." >&2
  exit 1
fi
if [[ "$#" -ne 3 ]]; then
  echo "Usage: $0 <staged-war> <release-id> <sha256>" >&2
  exit 1
fi

staged_war="$1"
release_id="$2"
expected_sha256="$3"
app_dir="/spring_module"
releases_dir="${app_dir}/releases"
current_link="${app_dir}/current.war"
health_url="http://127.0.0.1:11000/actuator/health/readiness"

cleanup() {
  rm -f -- "${staged_war}"
}
trap cleanup EXIT

if [[ ! -f "${staged_war}" ]]; then
  echo "Staged WAR does not exist: ${staged_war}" >&2
  exit 1
fi
if [[ ! "${release_id}" =~ ^[0-9a-f]{7,64}$ ]]; then
  echo "Release ID must be a Git commit hash." >&2
  exit 1
fi
if [[ "${staged_war}" != "/tmp/deepscan-${release_id}.war" || -L "${staged_war}" ]]; then
  echo "Staged WAR must be a regular, non-symlinked release file under /tmp." >&2
  exit 1
fi
if [[ ! "${expected_sha256}" =~ ^[0-9a-f]{64}$ ]]; then
  echo "Invalid SHA-256 value." >&2
  exit 1
fi

actual_sha256="$(sha256sum "${staged_war}" | awk '{print $1}')"
if [[ "${actual_sha256}" != "${expected_sha256}" ]]; then
  echo "WAR checksum verification failed." >&2
  exit 1
fi

install -d -m 0750 "${releases_dir}"
release_file="${releases_dir}/deepscan-${release_id}.war"
install -m 0644 "${staged_war}" "${release_file}"

previous_release=""
if [[ -L "${current_link}" ]]; then
  previous_release="$(readlink -f "${current_link}" || true)"
fi

activate_release() {
  local target="$1"
  local next_link="${app_dir}/.current.war.next"
  ln -sfn "${target}" "${next_link}"
  mv -Tf "${next_link}" "${current_link}"
}

activate_release "${release_file}"
systemctl restart deepscan.service

healthy=false
for _ in $(seq 1 30); do
  if curl --fail --silent --show-error "${health_url}" | grep -Eq '"status"[[:space:]]*:[[:space:]]*"UP"'; then
    healthy=true
    break
  fi
  sleep 2
done

if [[ "${healthy}" == true ]]; then
  echo "Deployment succeeded: ${release_id}"
  exit 0
fi

echo "Readiness check failed for ${release_id}. Starting rollback." >&2
if [[ -n "${previous_release}" && -f "${previous_release}" ]]; then
  activate_release "${previous_release}"
  systemctl restart deepscan.service
  echo "Rolled back to: ${previous_release}" >&2
else
  systemctl stop deepscan.service || true
  rm -f -- "${current_link}"
  echo "No previous release was available; the service was stopped." >&2
fi

journalctl -u deepscan.service --no-pager -n 80 >&2 || true
exit 1

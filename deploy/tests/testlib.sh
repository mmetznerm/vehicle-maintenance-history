#!/usr/bin/env bash
set -euo pipefail

fail() {
  printf 'FAIL: %s\n' "$*" >&2
  exit 1
}

assert_contains() {
  local haystack="$1"
  local needle="$2"
  [[ "$haystack" == *"$needle"* ]] || fail "expected output to contain: $needle"
}

assert_not_contains() {
  local haystack="$1"
  local needle="$2"
  [[ "$haystack" != *"$needle"* ]] || fail "expected output not to contain: $needle"
}

assert_file_mode() {
  local expected="$1"
  local path="$2"
  local actual
  actual="$(stat -c '%a' "$path")"
  [[ "$actual" == "$expected" ]] || fail "expected $path mode $expected, got $actual"
}

assert_file_exists() {
  local path="$1"
  [[ -f "$path" ]] || fail "expected file to exist: $path"
}

assert_executable() {
  local path="$1"
  [[ -x "$path" ]] || fail "expected executable file: $path"
}

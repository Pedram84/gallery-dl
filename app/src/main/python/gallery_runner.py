# -*- coding: utf-8 -*-
"""
Android execution runner and bridge for gallery-dl.
Preserves the complete upstream gallery-dl engine while providing structured
entry points, real-time stream interception, cooperative cancellation, and
metadata introspection for Android with lazy imports for fast, smooth startup.
"""

import sys
import io
import json
import logging
import traceback


class StreamEmitter:
    """Stream interceptor that forwards written lines to a Java/Kotlin callback with cancellation check."""
    def __init__(self, callback, is_err=False):
        self.callback = callback
        self.is_err = is_err
        self._buf = ""

    def write(self, s):
        if not s:
            return 0

        # Cooperative cancellation check
        if self.callback and hasattr(self.callback, "isCancelled") and self.callback.isCancelled():
            raise KeyboardInterrupt("Operation cancelled by user")

        self._buf += s
        while "\n" in self._buf:
            line, self._buf = self._buf.split("\n", 1)
            self._emit(line)
        return len(s)

    def _emit(self, line):
        if not self.callback:
            return
        try:
            if self.is_err:
                self.callback.onStderr(line)
            else:
                self.callback.onStdout(line)
        except Exception:
            pass

    def flush(self):
        if self._buf:
            self._emit(self._buf)
            self._buf = ""

    def reconfigure(self, **kwargs):
        """Supported for compatibility with gallery_dl.output.configure_standard_streams."""
        pass


def get_version():
    """Returns the version of gallery-dl without loading heavy modules."""
    try:
        from gallery_dl import version
        return str(version.__version__)
    except Exception:
        return "1.32.16"


def get_help_text():
    """Returns the full CLI help text formatted by gallery-dl's argument parser."""
    try:
        from gallery_dl import option
        parser = option.build_parser()
        return parser.format_help()
    except Exception as exc:
        return f"Unable to generate help: {exc}"


def list_extractors(filter_text="", limit=100):
    """
    Returns a JSON string of supported extractors matching filter, capped by limit for mobile efficiency.
    """
    try:
        from gallery_dl import extractor
        items = []
        filter_lower = (filter_text or "").strip().lower()
        count = 0
        for extr in extractor.extractors():
            name = getattr(extr, "__name__", "")
            cat = getattr(extr, "category", "") or ""
            subcat = getattr(extr, "subcategory", "") or ""
            example = getattr(extr, "example", "") or ""
            doc = (getattr(extr, "__doc__", "") or "").strip()

            if filter_lower:
                combined = f"{name} {cat} {subcat} {example} {doc}".lower()
                if filter_lower not in combined:
                    continue

            items.append({
                "name": name,
                "category": cat,
                "subcategory": subcat,
                "example": example,
                "description": doc,
            })
            count += 1
            if limit and limit > 0 and count >= limit:
                break

        return json.dumps(items)
    except Exception as exc:
        return json.dumps([{"name": "Error", "category": "error", "subcategory": "", "example": "", "description": str(exc)}])


def validate_config(config_text):
    """
    Validates a gallery-dl JSON configuration string.
    """
    try:
        parsed = json.loads(config_text)
        if not isinstance(parsed, dict):
            return json.dumps({"valid": False, "error": "Configuration root must be a JSON object"})
        return json.dumps({"valid": True, "error": None})
    except Exception as exc:
        return json.dumps({"valid": False, "error": str(exc)})


def run_gallery_dl(args, callback=None):
    """
    Executes the original gallery_dl.main() entry point with the provided argument list.
    Captures stdout and stderr, sends them to the callback, and returns the process exit code.
    Resets module-level states (logging handlers, configuration cache) before each execution.
    """
    import gallery_dl
    from gallery_dl import config, util

    # Suppress desktop configuration search paths that attempt to query /etc
    def _android_default_config(conf_type=None):
        config._type = conf_type or "json"
        config._load = util.json_loads
        config._default_configs = ()

    config.default = _android_default_config
    config._default_configs = ()

    old_stdin = sys.stdin
    old_stdout = sys.stdout
    old_stderr = sys.stderr
    old_argv = sys.argv
    emitter_out = StreamEmitter(callback, is_err=False)
    emitter_err = StreamEmitter(callback, is_err=True)

    # 1. Reset logging handlers to prevent duplication across successive executions
    root_logger = logging.getLogger()
    root_logger.handlers.clear()

    # 2. Reset config globals so options from previous jobs don't bleed into new ones
    try:
        config._config.clear()
        config._files.clear()
        config._default_configs = ()
    except Exception:
        pass

    try:
        sys.stdin = io.StringIO("")
        if callback is not None:
            sys.stdout = emitter_out
            sys.stderr = emitter_err

        # Prepare argv exactly as gallery-dl CLI receives them
        cmd_args = [str(a) for a in args]
        if "--config-ignore" not in cmd_args:
            cmd_args = ["--config-ignore"] + cmd_args
        sys.argv = ["gallery-dl"] + cmd_args

        # Call the original gallery-dl main entry point
        ret = gallery_dl.main()
        emitter_out.flush()
        emitter_err.flush()
        return ret if isinstance(ret, int) else 0

    except KeyboardInterrupt:
        emitter_out.flush()
        emitter_err._emit("Download cancelled by user.")
        emitter_err.flush()
        return 130

    except SystemExit as exc:
        emitter_out.flush()
        emitter_err.flush()
        if exc.code is None:
            return 0
        elif isinstance(exc.code, int):
            return exc.code
        else:
            emitter_err._emit(str(exc.code))
            return 1

    except Exception as exc:
        emitter_out.flush()
        err_msg = traceback.format_exc()
        emitter_err._emit(f"Error executing gallery-dl: {exc}")
        emitter_err._emit(err_msg)
        emitter_err.flush()
        return 1

    finally:
        sys.stdin = old_stdin
        sys.stdout = old_stdout
        sys.stderr = old_stderr
        sys.argv = old_argv
        root_logger.handlers.clear()
        try:
            config._config.clear()
            config._files.clear()
        except Exception:
            pass

"""Fail a release if a JAR or an embedded ZIP contains a Windows executable or batch file."""
import io
import sys
import zipfile


def inspect(archive, path):
    with zipfile.ZipFile(archive) as jar:
        for entry in jar.infolist():
            if entry.is_dir():
                continue
            name = f"{path}!/{entry.filename}"
            data = jar.read(entry)
            if entry.filename.lower().endswith((".bat", ".exe")) or data.startswith(b"MZ"):
                yield name
            nested = io.BytesIO(data)
            if zipfile.is_zipfile(nested):
                yield from inspect(nested, name)


if __name__ == "__main__":
    errors = [item for path in sys.argv[1:] for item in inspect(path, path)]
    if errors:
        sys.exit("\n".join(errors))

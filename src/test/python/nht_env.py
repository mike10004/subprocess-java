#!/usr/bin/env python
# -*- coding: utf-8 -*-

from __future__ import print_function
import sys
import os

"""Print values of environment variables"""

def find_case_insensitively(varname):
    varname = varname.lower()
    definitions = {}
    for key in os.environ.keys():
        if varname == key.lower():
            definitions[key] = os.getenv(key)
    return definitions

def main():
    from argparse import ArgumentParser
    parser = ArgumentParser()
    parser.add_argument("varnames", nargs='+', metavar="VAR", help="environment variable names to print")
    parser.add_argument("--dump", action="store_true", default=False, help="dump environment on stderr")
    parser.add_argument("--skip_undefined", action="store_true", default=False, help="ignore variable names that are not defined; otherwise exit with status 1")
    args = parser.parse_args()
    if args.dump:
        for varname in os.environ:
            print("{}={}".format(varname, os.getenv(varname)), file=sys.stderr)
    all_definitions = {}
    for varname in args.varnames:
        definitions = find_case_insensitively(varname)
        all_definitions.update(definitions)
    varnames = set(args.varnames) | set(all_definitions.keys())
    undefined = set()
    for varname in varnames:
        if varname in all_definitions:
            print("{}={}".format(varname, all_definitions[varname]))
        elif not args.skip_undefined:
            undefined.add(varname)
    if undefined:
        print("undefined:", ", ".join(undefined), file=sys.stderr)
        if not args.skip_undefined:
            return 1
    return 0

if __name__ == '__main__':
    exit(main())

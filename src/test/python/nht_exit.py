#!/usr/bin/env python
# -*- coding: utf-8 -*-

from __future__ import print_function
import sys

"""Exits with a status code specified by the first argument"""

if __name__ == '__main__':
    args = sys.argv[1:]
    code = 0
    if args:
        try:
            code = int(args[0])
        except ValueError:
            print("invalid argument; must be int", file=sys.stderr)
            code = 1
    exit(code)

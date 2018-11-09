#!/usr/bin/env python
# -*- coding: utf-8 -*-

from __future__ import print_function
import sys

"""Prints arguments, alternating standard output and standard error"""

if __name__ == '__main__':
    args = sys.argv[1:]
    for i in range(0, len(args), 2):
        print(args[i], file=sys.stdout)
        if i + 1 < len(args):
            print(args[i+1], file=sys.stderr)
    exit(0)

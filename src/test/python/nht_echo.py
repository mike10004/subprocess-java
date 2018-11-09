#!/usr/bin/env python
# -*- coding: utf-8 -*-

from __future__ import print_function
import sys

"""Like /bin/echo, except no newline is printed"""

if __name__ == '__main__':
    args = sys.argv[1:]
    print(' '.join(args), end="")
    exit(0)

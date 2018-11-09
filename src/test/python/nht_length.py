#!/usr/bin/env python3
# -*- coding: utf-8 -*-

from __future__ import print_function
import sys

"""Prints length of data received on standard input"""


if __name__ == '__main__':
    data = sys.stdin.buffer.read()
    print(len(data), end="")
    exit(0)

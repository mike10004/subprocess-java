#!/usr/bin/env python3
# -*- coding: utf-8 -*-

from __future__ import print_function

"""
Reads input lines and prints them until a blank line is entered. This is a 
lot like bin_cat.py except this:

* only reads from standard input,
* expects decoded bytes (as the input() function does), and 
* quits when when it reads an empty line.
"""

if __name__ == '__main__':
    while True:
        try:
            line = input()
        except EOFError:
            break
        if line:
            print(line)
        else:
            break
    exit(0)

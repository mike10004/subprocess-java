#!/usr/bin/env python3

from __future__ import print_function
import sys
import jinja2
import re
import glob
import logging
from collections import defaultdict
from argparse import ArgumentParser


_log = logging.getLogger('render_readme')
_RE_SNIPPET_BOOKEND = r'^\s*//\s*README_SNIPPET\s+(?P<id>\w+)\s*.*$'
_STATE_INSIDE = 'in'
_STATE_OUTSIDE = 'out'


class Snippet(object):

    def __init__(self, id, text):
        self.id = id
        assert id is not None
        self.text = text or ''

    @classmethod
    def load(cls, ifile):
        curr_id = None
        bucket = defaultdict(list)
        for line in ifile:
            m = re.match(_RE_SNIPPET_BOOKEND, line)
            if m:
                if curr_id is None:
                    curr_id = m.group('id')
                elif curr_id == m.group('id'):
                    curr_id = None
            else:
                if curr_id is not None:
                    bucket[curr_id].append(line)
        snippets = []
        for id in bucket:
            snippets.append(Snippet(id, ''.join(bucket[id])))
        return snippets


def build_model(args):
    model = {}
    for definition in args.definitions:
        definition = definition[0]
        key, value = definition.split('=', 2)
        model[key] = value
    if args.snippet_sources:
        snippets = []
        for pathname in glob.glob(args.snippet_sources):
            with open(pathname, 'r') as ifile:
                snippets += Snippet.load(ifile)
        for snippet in snippets:
            model[snippet.id] = snippet.text
    return model


def main():
    p = ArgumentParser()
    p.add_argument("template", help="template file to render")
    p.add_argument("-o", "--output", default="/dev/stdout", help="output file")
    p.add_argument("--define", dest="definitions", nargs=1, action='append', help="define a model property")
    p.add_argument("--snippet-sources", metavar="PATTERN", help="define snippet sources with a wildcard pattern")
    p.add_argument("--log-level", choices=('DEBUG', 'WARN', 'INFO', 'ERROR'), default='INFO', help="set log level")
    args = p.parse_args()
    logging.basicConfig(level=logging.__dict__[args.log_level])
    model = build_model(args)
    with open(args.template, 'r') as template_ifile:
        template_src = template_ifile.read()
    env = jinja2.Environment(variable_start_string='${', variable_end_string='}')
    template = env.from_string(template_src)
    rendering = template.render(model)
    with open(args.output, 'w') as ofile:
        print(rendering, file=ofile)
    return 0


if __name__ == '__main__':
    exit(main())

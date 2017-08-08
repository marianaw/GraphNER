"""
Reads trhough a file with one tweet per line and filters out all those 
tweets that are not in English.

Usage:
    filter_non_english.py -i <path> -o <path>
    filter_non_english.py -h | --help

Options:
    -i <path>   Path to input file.
    -o <path>   Path where to save English tweets.
    -h|--help   Show this screen.
"""
from docopt import docopt
from langdetect import detect


if __name__ == '__main__':
    opts = docopt(__doc__)
    input_path = opts['-i']
    output_path = opts['-o']
    
    f_input = open(input_path, 'r')
    f_output = open(output_path, 'w')
    count = 0
    for line in f_input:
        try:
            if detect(line) == 'en':
                f_output.write(line)
                count += 1
        except:
            pass
    
    print('Kept %d english tweets.' % count)

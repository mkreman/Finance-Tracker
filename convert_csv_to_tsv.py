#!/usr/bin/env python3
"""
Convert finance tracker CSV export to JSON or TSV format.
Extracts accounts from transactions and organizes data.
"""

import csv
import re
import json
from collections import OrderedDict
from datetime import datetime
import argparse


def parse_csv(csv_file):
    """Parse CSV file and return transactions."""
    transactions = []
    with open(csv_file, 'r', encoding='utf-8') as f:
        reader = csv.DictReader(f)
        # Normalize fieldnames to remove any extra whitespace
        if reader.fieldnames:
            reader.fieldnames = [name.strip() for name in reader.fieldnames]
        for row in reader:
            # Normalize keys to strip whitespace
            normalized_row = {k.strip(): v for k, v in row.items()}
            transactions.append(normalized_row)
    return transactions


def parse_datetime_to_timestamp(time_str):
    """Convert time string to milliseconds timestamp."""
    try:
        # Parse format: "Feb 01, 2026 12:04 PM" or "Feb 01, 2026 1:21 PM"
        dt = datetime.strptime(time_str.strip(), "%b %d, %Y %I:%M %p")
        # Convert to milliseconds timestamp
        timestamp = int(dt.timestamp() * 1000)
        return timestamp
    except ValueError:
        # Fallback if parsing fails
        return int(datetime.now().timestamp() * 1000)



def extract_accounts(transactions):
    """Extract unique accounts from transactions."""
    accounts = OrderedDict()
    
    # Account type mapping and default colors/icons
    # Note: Must match AccountType enum values in Android app: CASH, BANK, INVESTMENT, WALLET, PEOPLE, CUSTOM
    colors = {
        'BANK': '#2196F3',
        'WALLET': '#FF5722',  # Use WALLET for credit cards
        'CASH': '#FF9800',
    }
    icons = {
        'BANK': 'bank',
        'WALLET': 'wallet',  # Use WALLET for credit cards
        'CASH': 'wallet',
    }
    
    for transaction in transactions:
        account_str = transaction['ACCOUNT'].strip()
        
        # Handle transfers (e.g., "Bank HDFC->Bank SBI")
        if '->' in account_str:
            parts = account_str.split('->')
            account_str = parts[0].strip()
        
        if not account_str or account_str == '-':
            continue
        
        # Determine account type - must match Android app AccountType enum
        acc_type = 'BANK'  # default
        if 'Credit Card' in account_str or 'credit card' in account_str.lower():
            acc_type = 'WALLET'  # Map credit card to WALLET (valid enum value)
        elif 'Wallet' in account_str or 'wallet' in account_str.lower():
            acc_type = 'CASH'
        elif 'Cash' in account_str or 'cash' in account_str.lower():
            acc_type = 'CASH'
        
        # Normalize account name
        normalized_name = account_str.replace('1-Wallet', 'Wallet')
        
        if normalized_name not in accounts:
            accounts[normalized_name] = {
                'name': normalized_name,
                'type': acc_type,
                'initialBalance': 0,
                'currentBalance': 0,
                'colorHex': colors.get(acc_type, '#2196F3'),
                'iconKey': icons.get(acc_type, 'bank'),
            }
    
    return accounts


def parse_transaction_type(type_str):
    """Convert CSV type to JSON transaction type."""
    type_str = type_str.strip()
    if '(+)' in type_str:
        return 'INCOME'
    elif '(-)' in type_str:
        return 'EXPENSE'
    elif '(*)' in type_str:
        return 'TRANSFER'
    return 'EXPENSE'


def convert_to_json_transaction(transaction):
    """Convert CSV transaction to JSON format."""
    time_str = transaction['TIME'].strip()
    trans_type_str = transaction['TYPE'].strip()
    amount_str = transaction['AMOUNT'].strip()
    category = transaction['CATEGORY'].strip()
    account_str = transaction['ACCOUNT'].strip()
    notes = transaction['NOTES'].strip()
    
    # Parse transaction type
    trans_type = parse_transaction_type(trans_type_str)
    
    # Convert to integer amount
    try:
        amount = int(float(amount_str))
    except ValueError:
        amount = 0
    
    # Convert date to timestamp
    date_timestamp = parse_datetime_to_timestamp(time_str)
    
    # Normalize account names
    account_str = account_str.replace('1-Wallet', 'Wallet')
    
    # Base structure - note: toAccountName only added for TRANSFER transactions
    json_trans = {
        'date': date_timestamp,
        'type': trans_type,
        'totalAmount': amount,
        'note': notes,
        'payee': '',
        'fromAccountName': '',
        'splits': [],
    }
    
    if trans_type == 'TRANSFER':
        # Handle transfers - only TRANSFER type has toAccountName
        if '->' in account_str:
            parts = account_str.split('->')
            json_trans['fromAccountName'] = parts[0].strip().replace('1-Wallet', 'Wallet')
            json_trans['toAccountName'] = parts[1].strip().replace('1-Wallet', 'Wallet')
    else:
        # Handle income and expenses - no toAccountName field
        json_trans['fromAccountName'] = account_str.split('->')[0].strip() if '->' in account_str else account_str
        if category and category != '-':
            json_trans['payee'] = category
            json_trans['splits'].append({
                'categoryName': category,
                'amount': amount,
            })
    
    return json_trans


def write_json(output_file, accounts, budgets, transactions):
    """Write data to JSON file."""
    data = {
        'accounts': list(accounts.values()),
        'budgets': budgets,
        'transactions': transactions,
    }
    
    with open(output_file, 'w', encoding='utf-8') as f:
        json.dump(data, f, indent=4, ensure_ascii=False)


def normalize_transaction_tsv(transaction):
    """Normalize transaction for TSV output."""
    time_str = transaction['TIME'].strip()
    trans_type = transaction['TYPE'].strip()
    amount_str = transaction['AMOUNT'].strip()
    category = transaction['CATEGORY'].strip()
    account = transaction['ACCOUNT'].strip()
    notes = transaction['NOTES'].strip()
    
    # Handle transfers
    if '->' in account:
        # Keep as is for transfers
        pass
    
    # Normalize account names
    account = account.replace('1-Wallet', 'Wallet')
    
    # Convert amount to integer (remove decimals)
    try:
        amount = str(int(float(amount_str)))
    except ValueError:
        amount = amount_str
    
    # Normalize time format (convert to lowercase pm/am)
    time_str = re.sub(r' ([AP]M)$', lambda m: ' ' + m.group(1).lower(), time_str)
    
    return {
        'TIME': time_str,
        'TYPE': trans_type,
        'AMOUNT': amount,
        'CATEGORY': category,
        'ACCOUNT': account,
        'NOTES': notes,
    }


def write_tsv(output_file, accounts, budgets, transactions):
    """Write data to TSV file in the required format."""
    with open(output_file, 'w', encoding='utf-8', newline='') as f:
        # Write ACCOUNTS section
        f.write('### ACCOUNTS\n')
        f.write('NAME\tTYPE\tINITIAL_BALANCE\tCURRENT_BALANCE\tCOLOR\tICON\n')
        for account_name, account_info in accounts.items():
            f.write(f"{account_name}\t{account_info['type']}\t0\t0\t{account_info['colorHex']}\t{account_info['iconKey']}\n")
        
        f.write('\n')
        
        # Write BUDGETS section
        f.write('### BUDGETS\n')
        f.write('CATEGORY\tLIMIT_AMOUNT\tMONTH\tYEAR\n')
        for budget in budgets:
            f.write(f"{budget['category']}\t{budget['limit']}\t{budget['month']}\t{budget['year']}\n")
        
        f.write('\n')
        
        # Write TRANSACTIONS section
        f.write('### TRANSACTIONS\n')
        f.write('TIME\tTYPE\tAMOUNT\tCATEGORY\tACCOUNT\tNOTES\n')
        for transaction in transactions:
            line = f"{transaction['TIME']}\t{transaction['TYPE']}\t{transaction['AMOUNT']}\t{transaction['CATEGORY']}\t{transaction['ACCOUNT']}\t{transaction['NOTES']}\n"
            f.write(line)


def normalize_transaction(transaction):
    """Normalize transaction for TSV output."""
    time_str = transaction['TIME'].strip()
    trans_type = transaction['TYPE'].strip()
    amount_str = transaction['AMOUNT'].strip()
    category = transaction['CATEGORY'].strip()
    account = transaction['ACCOUNT'].strip()
    notes = transaction['NOTES'].strip()
    
    # Handle transfers
    if '->' in account:
        # Keep as is for transfers
        pass
    
    # Normalize account names
    account = account.replace('1-Wallet', 'Wallet')
    
    # Convert amount to integer (remove decimals)
    try:
        amount = str(int(float(amount_str)))
    except ValueError:
        amount = amount_str
    
    # Normalize time format (convert to lowercase pm/am)
    time_str = re.sub(r' ([AP]M)$', lambda m: ' ' + m.group(1).lower(), time_str)
    
    return {
        'TIME': time_str,
        'TYPE': trans_type,
        'AMOUNT': amount,
        'CATEGORY': category,
        'ACCOUNT': account,
        'NOTES': notes,
    }


def main():
    parser = argparse.ArgumentParser(description='Convert finance tracker CSV export to JSON or TSV format.')
    parser.add_argument('--input', '-i', default='export_08_02_26_708.csv', help='Input CSV file')
    parser.add_argument('--output', '-o', help='Output file (defaults based on format)')
    parser.add_argument('--format', '-f', choices=['json', 'tsv'], default='json', help='Output format (default: json)')
    parsed_args = parser.parse_args()
    
    csv_file = parsed_args.input
    output_format = parsed_args.format
    
    # Set default output filename based on format
    if not parsed_args.output:
        if output_format == 'json':
            output_file = 'converted_export.json'
        else:
            output_file = 'converted_export.tsv'
    else:
        output_file = parsed_args.output
    
    print(f"Reading CSV from {csv_file}...")
    transactions = parse_csv(csv_file)
    
    print(f"Found {len(transactions)} transactions")
    
    # Extract accounts
    accounts = extract_accounts(transactions)
    print(f"Extracted {len(accounts)} unique accounts: {list(accounts.keys())}")
    
    # Budgets (empty by default, can be populated manually)
    budgets = []
    
    # Convert based on format
    if output_format == 'json':
        # Convert transactions to JSON format
        json_transactions = [convert_to_json_transaction(t) for t in transactions]
        write_json(output_file, accounts, budgets, json_transactions)
        print(f"✓ JSON output written to {output_file}")
    else:
        # Convert transactions to TSV format
        tsv_transactions = [normalize_transaction_tsv(t) for t in transactions]
        write_tsv(output_file, accounts, budgets, tsv_transactions)
        print(f"✓ TSV output written to {output_file}")


if __name__ == '__main__':
    main()

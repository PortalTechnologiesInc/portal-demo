<script>
  import { sendWsMessage, messages } from '../socket.svelte.js';
  import { sessionToken } from '../state.svelte.js';
  import { onMount } from 'svelte';

  let amount = "9.99";
  let description = 'Test payment';
  let paymentType = 'single';
  let isSatsSelected = false;
  let currency = 'EUR';
  let paymentsHistory = [];
  let searchQuery = '';

  onMount(() => {
    sendWsMessage('RequestPaymentsHistory,' + $sessionToken);
  });

  function sendPayment() {
    if (isSatsSelected) {
      sendWsMessage('RequestSinglePayment,' + $sessionToken + ',Millisats,' + (amount + '000') + ',' + description + ',' + paymentType);
    } else {
      sendWsMessage('RequestSinglePayment,' + $sessionToken + ',' + currency + ',' + amount + ',' + description + ',' + paymentType);
    }
    // Reset form
    amount = "9.99";
    description = 'Test payment';
    isSatsSelected = false;
    currency = 'EUR';
  }

  $: if (!isSatsSelected) {
    if(!amount.includes('.')) {
      amount = amount + '.00';
    }
  }

  $: if (isSatsSelected) {
    if(amount.includes('.')) {
      amount = amount.slice(0, -3);
    }
  }

  $: if ($messages.length > 0) {
    let lastMessage = $messages[$messages.length - 1];
    if (lastMessage.cmd === 'PaymentsHistory') {
      console.log('PaymentsHistory', lastMessage.history);
      paymentsHistory = lastMessage.history;
    }
  }

  function formatPaymentStatus(payment) {
    if (payment.paid === undefined || payment.paid === null) {
      return 'Pending';
    }
    return payment.paid ? 'Paid' : 'Not Paid';
  }

  function formatPaymentStatusClass(payment) {
    if (payment.paid === undefined || payment.paid === null) {
      return '';
    }
    return payment.paid ? 'uk-badge-secondary' : 'uk-badge-destructive';
  }

  // Filter payments based on search query
  $: filteredPayments = paymentsHistory.filter(payment => {
    if (!searchQuery.trim()) return true;
    const query = searchQuery.toLowerCase();
    const currencyStr = (payment.currency === 'Millisats' ? 'Sats' : payment.currency).toLowerCase();
    const amountStr = String(payment.currency === 'Millisats' ? payment.amount / 1000 : payment.amount / 100).toLowerCase();
    const descriptionStr = payment.description?.toLowerCase() || '';
    const statusStr = formatPaymentStatus(payment).toLowerCase();
    const createdAtStr = payment.createdAt?.toLowerCase() || '';
    
    return currencyStr.includes(query) ||
           amountStr.includes(query) ||
           descriptionStr.includes(query) ||
           statusStr.includes(query) ||
           createdAtStr.includes(query);
  });
</script>

<div class="space-y-6">
  <div>
    <h3 class="text-lg font-medium">Payment</h3>
    <p class="text-muted-foreground text-sm">
      Manage your payment requests and history.
    </p>
  </div>
  <div class="border-border border-t"></div>

  <!-- Action bar with search, filter, and button -->
  <div class="flex items-center gap-3 mb-4">
    <div class="flex-1">
      <div class="uk-inline w-full">
        <span class="uk-form-icon">
          <uk-icon icon="search"></uk-icon>
        </span>
        <input
          class="uk-input w-full"
          type="text"
          placeholder="Search..."
          aria-label="Search payments"
          bind:value={searchQuery}
        />
      </div>
    </div>
    <button
      class="uk-btn uk-btn-primary"
      type="button"
      data-uk-toggle="target: #payment-modal"
    >
      <uk-icon icon="plus" class="mr-2"></uk-icon>
      Send Payment
    </button>
  </div>

  <!-- Payment History Table -->
  <div class="overflow-x-auto">
    <table class="uk-table uk-table-divider uk-table-small">
      <thead>
        <tr>
          <th>Currency</th>
          <th>Amount</th>
          <th>Description</th>
          <th>Status</th>
          <th>Created At</th>
        </tr>
      </thead>
      <tbody>
        {#each filteredPayments as payment}
          <tr>
            <td>
              <span class="uk-badge uk-badge-default">
                {payment.currency === 'Millisats' ? 'Sats' : payment.currency}
              </span>
            </td>
            <td>{payment.currency === 'Millisats' ? payment.amount / 1000 : payment.amount / 100}</td>
            <td>{payment.description}</td>
            <td>
              <span class="uk-badge {formatPaymentStatusClass(payment)}">
                {formatPaymentStatus(payment)}
              </span>
            </td>
            <td>{payment.createdAt}</td>
          </tr>
        {/each}
      </tbody>
    </table>
  </div>

  {#if filteredPayments.length > 0}
    <div class="text-sm text-muted-foreground mt-2">
      Page 1-{filteredPayments.length} of {filteredPayments.length} payments - {paymentsHistory.length} items
    </div>
  {/if}
</div>

<!-- Payment Modal -->
<div id="payment-modal" class="uk-modal-container" data-uk-modal>
  <div class="uk-modal-dialog uk-modal-body">
    <button
      class="uk-modal-close absolute right-4 top-4"
      type="button"
      data-uk-close
      aria-label="Close modal"
    ></button>
    <h2 class="uk-modal-title">Send Payment Request</h2>
    <p class="text-muted-foreground text-sm mt-2">
      Request a single payment.
    </p>
    <div class="border-border border-t mt-4 mb-4"></div>

    <div class="space-y-4">
      <div class="space-y-2">
        <label class="uk-form-label" for="modal-currency">Currency</label>
        <div class="flex items-center space-x-2">
          <input
            class="uk-toggle-switch uk-toggle-switch-primary"
            id="modal-toggle-switch"
            type="checkbox"
            bind:checked={isSatsSelected}
          />
          <label class="uk-form-label" for="modal-toggle-switch">Sats</label>
        </div>
        {#if !isSatsSelected}
          <input class="uk-input w-24" type="text" placeholder="EUR" bind:value={currency} />
        {/if}
        <div class="uk-form-help text-muted-foreground">
          Select the currency you want to pay in.
        </div>
      </div>

      <div class="space-y-2">
        <label class="uk-form-label" for="modal-amount">Amount</label>
        <input
          class="uk-input"
          id="modal-amount"
          type="text"
          placeholder="100"
          bind:value={amount}
        />
        <div class="uk-form-help text-muted-foreground">
          Enter the amount you want to pay.
        </div>
      </div>

      <div class="space-y-2">
        <label class="uk-form-label" for="modal-description">Description</label>
        <textarea
          class="uk-textarea"
          id="modal-description"
          placeholder="Test payment"
          bind:value={description}
        ></textarea>
        <div class="uk-form-help text-muted-foreground">
          Enter a description for the payment.
        </div>
      </div>

      <div class="flex justify-end gap-2 mt-6">
        <button
          class="uk-modal-close uk-btn uk-btn-default"
          type="button"
        >
          Cancel
        </button>
        <button
          class="uk-modal-close uk-btn uk-btn-primary"
          type="button"
          on:click={sendPayment}
        >
          Send Payment Request
        </button>
      </div>
    </div>
  </div>
</div>
